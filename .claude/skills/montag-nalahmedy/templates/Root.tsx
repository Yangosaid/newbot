import React from 'react';
import { Composition } from 'remotion';
import { Montage } from './Montage';
import { Reel, REEL_DURATION } from './Reel';
import { EditorialDemo, EDITORIAL_DURATION } from './EditorialDemo';
import { Trailer, TRAILER_DURATION } from './Trailer';
import { Thumb } from './Thumb';
import { Rain, RAIN_DURATION, RainTitlesDV, RainTitlesDVSfx, DV_END } from './Rain';

export const Root: React.FC = () => (
  <>
    <Composition id="Montage" component={Montage} durationInFrames={528} fps={30} width={1920} height={1080} />
    <Composition id="Rain" component={Rain} durationInFrames={RAIN_DURATION} fps={30} width={3840} height={2160} />
    <Composition id="RainTitlesDV" component={RainTitlesDV} durationInFrames={DV_END} fps={30} width={3840} height={2160} />
    <Composition id="RainTitlesDVSfx" component={RainTitlesDVSfx} durationInFrames={DV_END} fps={30} width={1920} height={1080} />
    <Composition id="Reel" component={Reel} durationInFrames={REEL_DURATION} fps={30} width={1080} height={1920} />
    <Composition id="EditorialDemo" component={EditorialDemo} durationInFrames={EDITORIAL_DURATION} fps={30} width={1080} height={1920} />
    <Composition id="Trailer" component={Trailer} durationInFrames={TRAILER_DURATION} fps={24} width={3840} height={2160} />
    <Composition id="Thumb" component={Thumb} durationInFrames={1} fps={24} width={1280} height={720} />
  </>
);
