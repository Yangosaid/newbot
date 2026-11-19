// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

use jni::objects::{JClass, JObject, JString};
use jni::sys::{jfloat, jint, JNI_ERR};
use jni::JNIEnv;
use jni::{JavaVM, NativeMethod};
use log::{error, info, debug};
use ndk_sys;
use std::ffi::c_void;

use std::sync::atomic::{AtomicBool, Ordering};
use std::thread;

use android_logger::Config;

use std::fs::File;
use std::process::{Command, Stdio};

mod input;
mod renderer_bindings;

/// ## Examples
/// ```
/// let method:NativeMethod = jni_method!(native_method, "(Ljava/lang/String;)V");
/// ```
macro_rules! jni_method {
    ( $name: tt, $method:tt, $signature:expr ) => {{
        jni::NativeMethod {
            name: jni::strings::JNIString::from(stringify!($name)),
            sig: jni::strings::JNIString::from($signature),
            fn_ptr: $method as *mut c_void,
        }
    }};
}

static RENDERER_STARTED: AtomicBool = AtomicBool::new(false);

extern "system" fn renderer_init<'local>(
    mut env: JNIEnv<'local>,
    _clz: JClass<'local>,
    surface: JObject<'local>,
    loader: JString<'local>,
    xdpi: jfloat,
    ydpi: jfloat,
    fps: jint,
) {
    debug!("renderer_init");
    let window = unsafe { ndk_sys::ANativeWindow_fromSurface(env.get_native_interface() as *mut _, surface.as_raw() as *mut _) };

    let window = match std::ptr::NonNull::new(window) {
        Some(x) => x,
        None => {
            error!("ANativeWindow_fromSurface was null!");
            return;
        }
    };

    let window = unsafe { ndk::native_window::NativeWindow::from_ptr(window) };

    let width = window.width();
    let height = window.height();

    info!(
        "renderer_init width: {}, height: {}, fps: {}",
        width, height, fps
    );

    if RENDERER_STARTED.compare_exchange(false, true, 
        Ordering::Acquire, Ordering::Relaxed).is_err() {
        let win = window.ptr().as_ptr() as *mut c_void;
        unsafe {
            renderer_bindings::setNativeWindow(win);
            renderer_bindings::resetSubWindow(win, 0, 0, width, height, width, height, 1.0, 0.0);
        }
    } else {
        input::start_input_system(width, height);

        thread::spawn(move || {
            let win = window.ptr().as_ptr() as *mut c_void;
            info!("win: {:#?}", win);
            unsafe {
                renderer_bindings::startOpenGLRenderer(
                    win,
                    width,
                    height,
                    xdpi as i32,
                    ydpi as i32,
                    fps as i32,
                );
            }
        });

        let loader_path: String = match env.get_string(&loader) {
            Ok(s) => s.into(),
            Err(e) => {
                error!("invalid loader path: {:?}", e);
                return;
            }
        };
        let working_dir = "/data/data/io.twoyi/rootfs";
        let log_path = "/data/data/io.twoyi/log.txt";
        let outputs = match File::create(log_path) {
            Ok(f) => f,
            Err(e) => {
                error!("cannot create {}: {}", log_path, e);
                return;
            }
        };
        let errors = outputs.try_clone().expect("dup log fd");
        match Command::new("./init")
            .current_dir(working_dir)
            .env("TYLOADER", loader_path)
            .stdout(Stdio::from(outputs))
            .stderr(Stdio::from(errors))
            .spawn()
        {
            Ok(child) => info!("container init started, pid {}", child.id()),
            Err(e) => error!("failed to start container init: {}", e),
        }
    }
}

extern "system" fn renderer_reset_window<'local>(
    env: JNIEnv<'local>,
    _clz: JClass<'local>,
    surface: JObject<'local>,
    _top: jint,
    _left: jint,
    _width: jint,
    _height: jint,
) {
    debug!("reset_window");
    unsafe {
        let window = ndk_sys::ANativeWindow_fromSurface(env.get_native_interface() as *mut _, surface.as_raw() as *mut _);
        renderer_bindings::resetSubWindow(window as *mut c_void, 0, 0, _width, _height, _width, _height, 1.0, 0.0);
    }
}

extern "system" fn renderer_remove_window<'local>(env: JNIEnv<'local>, _clz: JClass<'local>, surface: JObject<'local>) {
    debug!("renderer_remove_window");

    unsafe {
        let window = ndk_sys::ANativeWindow_fromSurface(env.get_native_interface() as *mut _, surface.as_raw() as *mut _);
        renderer_bindings::removeSubWindow(window as *mut c_void);
    }
}

extern "system" fn handle_touch<'local>(mut env: JNIEnv<'local>, _clz: JClass<'local>, event: JObject<'local>) {
    // MotionEvent.mNativePtr holds the native AInputEvent*.
    let p = match env.get_field(&event, "mNativePtr", "J").and_then(|v| v.j()) {
        Ok(p) => p,
        Err(_) => {
            let _ = env.exception_clear();
            return;
        }
    };
    if let Some(nonptr) = std::ptr::NonNull::new(p as *mut ndk_sys::AInputEvent) {
        let ev = unsafe { ndk::event::MotionEvent::from_ptr(nonptr) };
        // A panic must never unwind into the JVM.
        let _ = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| input::handle_touch(ev)));
    }
}

extern "system" fn send_key_code<'local>(_env: JNIEnv<'local>, _clz: JClass<'local>, keycode: jint) {
    debug!("send key code!");
    let _ = std::panic::catch_unwind(|| input::send_key_code(keycode));
}

unsafe fn register_natives(jvm: &JavaVM, class_name: &str, methods: &[NativeMethod]) -> jint {
    let mut env: JNIEnv = match jvm.get_env() {
        Ok(e) => e,
        Err(_) => return JNI_ERR,
    };
    let jni_version = env.get_version().unwrap_or(jni::JNIVersion::V6);
    let version: jint = jni_version.into();

    debug!("JNI Version : {:#?} ", jni_version);

    let clazz = match env.find_class(class_name) {
        Ok(clazz) => clazz,
        Err(e) => {
            error!("java class not found : {:?}", e);
            return JNI_ERR;
        }
    };
    debug!("clazz: {:#?}", clazz);

    let result = env.register_native_methods(&clazz, methods);

    if result.is_ok() {
        debug!("register_natives : succeed");
        version
    } else {
        error!("register_natives : failed ");
        JNI_ERR
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn JNI_OnLoad(jvm: JavaVM, _reserved: *mut c_void) -> jint {
    android_logger::init_once(
        Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("CLIENT_EGL"),
    );

    debug!("JNI_OnLoad");

    // Writing to a socket the guest closed must not kill the whole app.
    libc::signal(libc::SIGPIPE, libc::SIG_IGN);

    let class_name: &str = "io/twoyi/Renderer";
    let jni_methods = [
        jni_method!(init, renderer_init, "(Landroid/view/Surface;Ljava/lang/String;FFI)V"),
        jni_method!(
            resetWindow,
            renderer_reset_window,
            "(Landroid/view/Surface;IIII)V"
        ),
        jni_method!(
            removeWindow,
            renderer_remove_window,
            "(Landroid/view/Surface;)V"
        ),
        jni_method!(handleTouch, handle_touch, "(Landroid/view/MotionEvent;)V"),
        jni_method!(sendKeycode, send_key_code, "(I)V"),
    ];

    register_natives(&jvm, class_name, jni_methods.as_ref())
}
