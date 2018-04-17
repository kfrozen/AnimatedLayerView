package com.github.troy.animatedlayers;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;

import static com.github.troy.animatedlayers.AnimatedLayerView.FILL_PARENT;
import static com.github.troy.animatedlayers.AnimatedLayerView.NO_SCALE;

/**
 * Created by troy on 2018/4/10.
 */

public class LayerConfig {
    /*
 * This is the default value for animationInterval field.
 * In case of translate animation, this value means the difference of
 * drawable and view's size will be regarded as the interval.
 * And in case of rotation, 360 will be the default interval.
 * While for scale, it will be 1;
*/
    public static final int ANIMATION_INTERVAL_AUTO = -1;

    private Builder builder;

    public LayerConfig(Builder builder) {
        this.builder = builder;
    }

    @DrawableRes
    public int getDrawableResId() {
        return builder.resId;
    }

    @AnimatedLayerView.AnimationType
    public int getAnimationType() {
        return builder.animationType;
    }

    @AnimatedLayerView.LayerScaleType
    public int getLayerScaleType() {
        return builder.layerScaleType;
    }

    public int getLayerGravity() {
        return builder.layerGravity;
    }

    public int getMarginStart() {
        return builder.marginStart;
    }

    public int getMarginTop() {
        return builder.marginTop;
    }

    public int getMarginEnd() {
        return builder.marginEnd;
    }

    public int getMarginBottom() {
        return builder.marginBottom;
    }

    public float getFromValue() {
        return builder.fromValue;
    }

    public float getAnimationInterval() {
        return builder.animationInterval;
    }

    public int getDuration() {
        return builder.duration;
    }

    public int getRepeatMode() {
        return builder.repeatMode;
    }

    public int getRepeatCount() {
        return builder.repeatCount;
    }

    public TimeInterpolator getTimeInterpolator() {
        return builder.interpolator;
    }

    public static class Builder {
        @DrawableRes
        int resId = -1;
        @AnimatedLayerView.AnimationType
        int animationType;
        @AnimatedLayerView.LayerScaleType
        int layerScaleType = NO_SCALE;
        int layerGravity = FILL_PARENT;
        int marginStart = 0;
        int marginTop = 0;
        int marginEnd = 0;
        int marginBottom = 0;
        float fromValue = 0;
        float animationInterval = ANIMATION_INTERVAL_AUTO;
        int duration;
        int repeatMode = ValueAnimator.RESTART;
        int repeatCount = ValueAnimator.INFINITE;
        TimeInterpolator interpolator = null;

        public Builder(@DrawableRes int resId, @AnimatedLayerView.AnimationType int animationType) {
            this.resId = resId;
            this.animationType = animationType;
        }

        public Builder fromValue(float fromValue) {
            this.fromValue = fromValue;
            return this;
        }

        public Builder animationInterval(@FloatRange(from = 1f) float animationInterval) {
            this.animationInterval = animationInterval;
            return this;
        }

        public Builder layerGravity(int layerGravity) {
            this.layerGravity = layerGravity;
            return this;
        }

        public Builder layerScaleType(@AnimatedLayerView.LayerScaleType int layerScaleType) {
            this.layerScaleType = layerScaleType;
            return this;
        }

        public Builder duration(@IntRange(from = 0) int duration) {
            this.duration = duration;
            return this;
        }

        public Builder repeatMode(int repeatMode) {
            if(repeatMode == ValueAnimator.RESTART || repeatMode == ValueAnimator.REVERSE) {
                this.repeatMode = repeatMode;
            }
            return this;
        }

        public Builder repeatCount(int repeatCount) {
            if(repeatCount > 0) {
                this.repeatCount = repeatCount;
            }
            return this;
        }

        public Builder interpolator(TimeInterpolator interpolator) {
            this.interpolator = interpolator;
            return this;
        }

        public Builder margin(int start, int top, int end, int bottom) {
            this.marginStart = start;
            this.marginTop = top;
            this.marginEnd = end;
            this.marginBottom = bottom;
            return this;
        }

        public LayerConfig build() {
            return new LayerConfig(this);
        }
    }
}
