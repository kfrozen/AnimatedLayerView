package com.github.troy.animatedlayers;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Created by troy on 2018/4/10.
 */

public class AnimatedLayerView extends View {
    /*****Animation Type*****/
    public static final int NO_ANIMATION = 0;
    public static final int TRANSLATE_START = 1;
    public static final int TRANSLATE_END = 2;
    public static final int TRANSLATE_UP = 3;
    public static final int TRANSLATE_DOWN = 4;
    public static final int ROTATE_CLOCKWISE = 5;
    public static final int ROTATE_ANTICLOCKWISE = 6;
    public static final int SCALE = 7;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NO_ANIMATION, TRANSLATE_START, TRANSLATE_END, TRANSLATE_UP, TRANSLATE_DOWN, ROTATE_CLOCKWISE, ROTATE_ANTICLOCKWISE, SCALE})
    public @interface AnimationType
    {
    }
    /*****Animation Type ends*****/

    /*****Layer Gravity*****/
    public static final int FILL_PARENT = 1;
    public static final int ALIGN_TOP = 1<<1;
    public static final int ALIGN_BOTTOM = 1<<2;
    public static final int ALIGN_START = 1<<3;
    public static final int ALIGN_END = 1<<4;
    public static final int CENTER = 1<<5;
    public static final int CENTER_VERTICAL = 1<<6;
    public static final int CENTER_HORIZONTAL = 1<<7;
    /*****Layer Gravity ends*****/

    /*****ScaleType*****/
    /*****Note that LayerScaleType can only be counted when layerGravity is CENTER or MATCH_PARENT*****/
    public static final int FITXY = 1;
    public static final int CENTER_INSIDE = 2;
    public static final int CENTER_CROP = 3;
    public static final int NO_SCALE = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FITXY, CENTER_INSIDE, CENTER_CROP, NO_SCALE})
    public @interface LayerScaleType
    {
    }
    /*****ScaleType ends*****/

    private int vWidth;
    private int vHeight;
    private Paint paint;
    private ArrayList<Layer> layerInfoList = new ArrayList<>();

    public AnimatedLayerView(Context context) {
        super(context);
        init();
    }

    public AnimatedLayerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedLayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AnimatedLayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
    }


    /**
     * @param layerConfig Config for the adding layer
     * @return The index of the added layer in the layer list,
     *         which can be applied when this layer need to be removed {@link #removeLayer(int)}}.
     * NOTE: This method will NOT check the duplication of the layerConfigs.
     * */
    public int addLayer(LayerConfig layerConfig) {
        if (layerConfig == null) {
            return -1;
        }
        layerInfoList.add(Layer.generate(layerConfig));
        reconfiguration(false);
        return layerInfoList.size() - 1;
    }

    /**
     * @param layerConfigs Config list for the adding layers
     * @return The new index of the first item from the input list after composed to the existing list,
     *         which can be applied when a layer need to be removed {@link #removeLayer(int)}}.
     * NOTE: This method will NOT check the duplication of the layerConfigs.
     * */
    public int addLayerList(ArrayList<LayerConfig> layerConfigs) {
        if (layerConfigs == null || layerConfigs.isEmpty()) {
            return -1;
        }
        final int resultIndex = layerInfoList.size();
        for (LayerConfig config : layerConfigs) {
            if (config == null) {
                continue;
            }
            layerInfoList.add(Layer.generate(config));
        }
        reconfiguration(false);
        return resultIndex;
    }

    /**
     * @param layerConfigs Config list for the adding layers
     * NOTE: This method is different from {@link #addLayerList(ArrayList)} ,the input layers will
     *                     totally replace the current layers if there was any.
     * */
    public void replaceLayerList(ArrayList<LayerConfig> layerConfigs) {
        if (layerConfigs == null || layerConfigs.isEmpty()) {
            return;
        }
        if (layerInfoList != null && !layerInfoList.isEmpty()) {
            for (Layer layer : layerInfoList) {
                if (layer == null) {
                    continue;
                }
                layer.destroy();
            }
            layerInfoList.clear();
        }
        if (layerInfoList == null) {
            layerInfoList = new ArrayList<>();
        }
        for (LayerConfig config : layerConfigs) {
            if (config == null) {
                continue;
            }
            layerInfoList.add(Layer.generate(config));
        }
        reconfiguration(true);
    }

    /**
     * @param layerIndex The index of the removing layer in the layer list,
     *                   which should be returned by {@link #addLayer(LayerConfig)}}
     * */
    public void removeLayer(int layerIndex) {
        if (layerInfoList == null || layerIndex < 0 || layerIndex >= layerInfoList.size()) {
            return;
        }
        Layer layer = layerInfoList.remove(layerIndex);
        layer.destroy();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if(w == oldw && h == oldh) return;

        vWidth = w;
        vHeight = h;

        reconfiguration(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (layerInfoList != null && !layerInfoList.isEmpty()) {
            canvas.save();
            for (Layer info : layerInfoList) {
                if (info == null || !info.configured) {
                    continue;
                }
                paint.setShader(info.layerShader);
                if (info.animationType == AnimatedLayerView.ROTATE_CLOCKWISE
                        || info.animationType == AnimatedLayerView.ROTATE_ANTICLOCKWISE) {
                    canvas.drawCircle(info.targetRect.centerX(), info.targetRect.centerY(),
                            Math.min(info.targetRect.width(), info.targetRect.height())/2, paint);
                } else {
                    canvas.drawRect(info.targetRect, paint);
                }
            }
            canvas.restore();
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (layerInfoList != null && !layerInfoList.isEmpty()) {
            for (Layer info : layerInfoList) {
                if (info == null || !info.configured) {
                    continue;
                }
                if (visibility == VISIBLE) {
                    info.tryStart();
                } else {
                    info.tryEnd();
                }
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroy();
    }

    private void destroy() {
        if (layerInfoList != null && !layerInfoList.isEmpty()) {
            for (Layer info : layerInfoList) {
                if (info == null) {
                    continue;
                }
                info.destroy();
            }
            layerInfoList.clear();
        }
    }

    private void configLayerInfo(@NonNull final Layer info) {
        //Config bitmap shader
        if(info.layerShader == null) {
            Bitmap bitmap = ((BitmapDrawable)getResources().getDrawable(info.resId)).getBitmap();
            info.drawableWidth = bitmap.getWidth();
            info.drawableHeight = bitmap.getHeight();
            Shader.TileMode x = (info.animationType == TRANSLATE_START || info.animationType == TRANSLATE_END)
                    ? ((info.drawableWidth < vWidth) ? Shader.TileMode.CLAMP : Shader.TileMode.REPEAT) : Shader.TileMode.CLAMP;
            Shader.TileMode y = (info.animationType == TRANSLATE_UP || info.animationType == TRANSLATE_DOWN)
                    ? ((info.drawableHeight < vHeight) ? Shader.TileMode.CLAMP : Shader.TileMode.REPEAT) : Shader.TileMode.CLAMP;
            info.layerShader = new BitmapShader(bitmap, x, y);
        }
        //Config Matrix and target Rect to draw
        info.targetRect = new Rect(0, 0, vWidth, vHeight);
        if (info.layerGravity == CENTER) {
            int widthOffset = vWidth - info.drawableWidth > 0 ? (vWidth - info.drawableWidth)/2 : 0;
            int heightOffset = vHeight - info.drawableHeight > 0 ? (vHeight - info.drawableHeight)/2 : 0;
            if (info.animationType == ROTATE_CLOCKWISE || info.animationType == ROTATE_ANTICLOCKWISE) {
                info.targetRect = new Rect(widthOffset, heightOffset, vWidth - widthOffset, vHeight - heightOffset);
            }
            float scaleX = vWidth / (info.drawableWidth*1f);
            float scaleY = vHeight / (info.drawableHeight*1f);
            //Only scale down, no scale up for gravity == CENTER
            if (info.layerScaleType == NO_SCALE) {
                info.matrix.setTranslate(widthOffset, heightOffset);
            } else if (info.layerScaleType == FITXY) {
                float actualScaleX = Math.min(1, scaleX);
                float actualScaleY = Math.min(1, scaleY);
                info.matrix.setScale(actualScaleX, actualScaleY);
                info.matrix.postTranslate(widthOffset*actualScaleX, heightOffset*actualScaleY);
            } else {
                float actualScaleX = Math.min(1, scaleX);
                float actualScaleY = Math.min(1, scaleY);
                float actualScale = info.layerScaleType == CENTER_INSIDE ?
                        Math.min(actualScaleX, actualScaleY) : Math.max(actualScaleX, actualScaleY);
                info.matrix.setScale(actualScale, actualScale);
                info.matrix.postTranslate(widthOffset*actualScale, heightOffset*actualScale);
            }
        } else if (info.layerGravity == FILL_PARENT){
            if(info.layerScaleType != NO_SCALE) {
                float scaleX = vWidth / (info.drawableWidth*1f);
                float scaleY = vHeight / (info.drawableHeight*1f);
                if (info.layerScaleType == FITXY) {
                    info.matrix.setScale(scaleX, scaleY);
                } else {
                    float actualScale = info.layerScaleType == CENTER_INSIDE ?
                            Math.min(scaleX, scaleY) : Math.max(scaleX, scaleY);
                    info.matrix.setScale(actualScale, actualScale);
                }
            }
        } else {
            info.matrix.setTranslate(0, 0);
            if ((info.layerGravity & CENTER_HORIZONTAL) == CENTER_HORIZONTAL) {
                int widthOffset = vWidth - info.drawableWidth > 0 ? (vWidth - info.drawableWidth)/2 : 0;
                info.targetRect.top = info.marginTop;
                info.matrix.postTranslate(widthOffset, 0);
            } else if ((info.layerGravity & CENTER_VERTICAL) == CENTER_VERTICAL) {
                int heightOffset = vHeight - info.drawableHeight > 0 ? (vHeight - info.drawableHeight)/2 : 0;
                info.targetRect.left = info.marginStart;
                info.matrix.postTranslate(0, heightOffset);
            }
            if ((info.layerGravity & ALIGN_TOP) == ALIGN_TOP) {
                if (info.animationType != TRANSLATE_UP && info.animationType != TRANSLATE_DOWN) {
                    info.targetRect.top = info.marginTop;
                    info.targetRect.bottom = Math.min(info.drawableHeight + info.marginTop, vHeight);
                }
                info.matrix.postTranslate(0, info.marginTop);
            } else if ((info.layerGravity & ALIGN_BOTTOM) == ALIGN_BOTTOM) {
                int heightOffset = vHeight - info.drawableHeight > 0 ? vHeight - info.drawableHeight : 0;
                if (info.animationType != TRANSLATE_UP && info.animationType != TRANSLATE_DOWN) {
                    info.targetRect.top = heightOffset - info.marginBottom;
                    info.targetRect.bottom = vHeight - info.marginBottom;
                    info.matrix.postTranslate(0, info.targetRect.top);
                }
                info.matrix.postTranslate(0, heightOffset - info.marginBottom);
            }
            if ((info.layerGravity & ALIGN_START) == ALIGN_START) {
                if (info.animationType != TRANSLATE_START && info.animationType != TRANSLATE_END) {
                    info.targetRect.left = info.marginStart;
                    info.targetRect.right = Math.min(info.drawableWidth + info.marginStart, vWidth);
                }
                info.matrix.postTranslate(info.marginStart, 0);
            } else if ((info.layerGravity & ALIGN_END) == ALIGN_END) {
                int widthOffset = vWidth - info.drawableWidth > 0 ? vWidth - info.drawableWidth : 0;
                if (info.animationType != TRANSLATE_START && info.animationType != TRANSLATE_END) {
                    info.targetRect.left = widthOffset - info.marginEnd;
                    info.targetRect.right = vWidth - info.marginEnd;
                }
                info.matrix.postTranslate(widthOffset - info.marginEnd, 0);
            }
        }
        info.layerShader.setLocalMatrix(info.matrix);
        //Config Animator
        if (info.animationInterval == LayerConfig.ANIMATION_INTERVAL_AUTO && info.animationType != NO_ANIMATION) {
            if (info.animationType == TRANSLATE_START || info.animationType == TRANSLATE_END) {
                int interval = Math.abs(info.drawableWidth - vWidth);
                if(interval != 0) {
                    info.valueAnimator = ValueAnimator.ofInt(0, interval);
                    info.animationInterval = interval;
                }
            } else if (info.animationType == TRANSLATE_UP || info.animationType == TRANSLATE_DOWN) {
                int interval = Math.abs(info.drawableHeight - vHeight);
                if(interval != 0) {
                    info.valueAnimator = ValueAnimator.ofInt(0, interval);
                    info.animationInterval = interval;
                }
            } else if (info.animationType == ROTATE_CLOCKWISE || info.animationType == ROTATE_ANTICLOCKWISE) {
                info.valueAnimator = ValueAnimator.ofFloat(info.fromValue, 360 + info.fromValue);
                info.animationInterval = 360;
            } else if (info.animationType == SCALE) {
                info.valueAnimator = ValueAnimator.ofFloat(0f, 1f);
                info.animationInterval = 1;
            }
            if (info.valueAnimator != null) {
                info.valueAnimator.setDuration(info.duration);
                info.valueAnimator.setRepeatMode(info.repeatMode);
                info.valueAnimator.setRepeatCount(info.repeatCount);
                info.valueAnimator.setInterpolator(info.interpolator);
            }
        }
        //Set initial value
        if (info.animationType == ROTATE_CLOCKWISE || info.animationType == ROTATE_ANTICLOCKWISE) {
            if (info.fromValue != 0) {
                info.matrix.postRotate(info.fromValue, info.targetRect.centerX(), info.targetRect.centerY());
            }
        } else if (info.animationType == TRANSLATE_START || info.animationType == TRANSLATE_END) {
            if (info.fromValue != 0) {
                info.matrix.postTranslate(info.fromValue, 0);
            }
        } else if (info.animationType == TRANSLATE_UP || info.animationType == TRANSLATE_DOWN) {
            if (info.fromValue != 0) {
                info.matrix.postTranslate(0, info.fromValue);
            }
        }

        if (info.valueAnimator != null) {
            info.valueAnimator.cancel();
            info.valueAnimator.removeAllUpdateListeners();
            if(info.animationType != NO_ANIMATION) {
                info.valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    float lastFraction = 0f;
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float fraction = animation.getAnimatedFraction();
                        float change;
                        if (animation.getRepeatMode() == ValueAnimator.RESTART) {
                            if (fraction > lastFraction) {
                                change = (fraction - lastFraction) * info.animationInterval;
                            } else {
                                change = fraction * info.animationInterval;
                            }
                        } else {
                            change = (fraction - lastFraction) * info.animationInterval;
                        }
                        lastFraction = fraction;

                        if (info.animationType == TRANSLATE_START) {
                            info.matrix.postTranslate(-change, 0);
                        } else if (info.animationType == TRANSLATE_END) {
                            info.matrix.postTranslate(change, 0);
                        } else if (info.animationType == TRANSLATE_UP) {
                            info.matrix.postTranslate(0, -change);
                        } else if (info.animationType == TRANSLATE_DOWN) {
                            info.matrix.postTranslate(0, change);
                        } else if (info.animationType == ROTATE_CLOCKWISE) {
                            info.matrix.postRotate(change, info.targetRect.centerX(), info.targetRect.centerY());
                        } else if (info.animationType == ROTATE_ANTICLOCKWISE) {
                            info.matrix.postRotate(-change, info.targetRect.centerX(), info.targetRect.centerY());
                        } else if (info.animationType == SCALE) {
                            info.matrix.setScale(change, change);
                        }

                        info.layerShader.setLocalMatrix(info.matrix);
                        invalidate();
                    }
                });
                info.valueAnimator.start();
            }
        }
        info.configured = true;
    }

    /**
     * @param forceConfigAll true means the view params have changed and all layers need to be reconfigured.
     *                       false means only some of the layers need reconfiguration or new layer was added.
     * */
    private void reconfiguration(boolean forceConfigAll) {
        if (vWidth <= 0 || vHeight <= 0) { //View is not ready yet
            return;
        }
        if (layerInfoList.isEmpty()) { //No layers added
            return;
        }
        for (Layer info : layerInfoList) {
            if (info ==  null || (info.configured && !forceConfigAll)) {
                continue;
            }
            configLayerInfo(info);
        }
    }

    private static class Layer {
        @AnimationType int animationType;
        @DrawableRes
        int resId;
        @LayerScaleType int layerScaleType;
        int layerGravity;
        int marginStart;
        int marginTop;
        int marginEnd;
        int marginBottom;
        BitmapShader layerShader;
        int drawableWidth;
        int drawableHeight;
        Matrix matrix;
        Rect targetRect;
        ValueAnimator valueAnimator;
        float animationInterval;
        float fromValue;
        int duration;
        int repeatMode = ValueAnimator.RESTART;
        int repeatCount = ValueAnimator.INFINITE;
        TimeInterpolator interpolator = null;
        boolean configured = false;

        public static Layer generate(LayerConfig config) {
            Layer info = new Layer();
            info.animationType = config.getAnimationType();
            info.layerGravity = config.getLayerGravity();
            info.layerScaleType = config.getLayerScaleType();
            info.resId = config.getDrawableResId();
            info.marginStart = config.getMarginStart();
            info.marginTop = config.getMarginTop();
            info.marginEnd = config.getMarginEnd();
            info.marginBottom = config.getMarginBottom();
            info.matrix = new Matrix();
            if (info.animationType != NO_ANIMATION) {
                if (config.getAnimationInterval() != LayerConfig.ANIMATION_INTERVAL_AUTO) {
                    info.valueAnimator = ValueAnimator.ofFloat(config.getFromValue(), config.getFromValue() + config.getAnimationInterval());
                    if(config.getDuration() > 0) info.valueAnimator.setDuration(config.getDuration());
                    info.valueAnimator.setRepeatMode(config.getRepeatMode());
                    info.valueAnimator.setRepeatCount(config.getRepeatCount());
                    info.valueAnimator.setInterpolator(config.getTimeInterpolator());
                }
                info.animationInterval = config.getAnimationInterval();
                info.fromValue = config.getFromValue();
                info.duration = config.getDuration();
                info.repeatMode = config.getRepeatMode();
                info.repeatCount = config.getRepeatCount();
                info.interpolator = config.getTimeInterpolator();
            }

            return info;
        }

        private void destroy() {
            if(valueAnimator != null) {
                valueAnimator.cancel();
                valueAnimator = null;
            }
            matrix = null;
            layerShader = null;
            configured = false;
        }

        /**
         * @return true if the contained value animator was started by this action, otherwise false.
         * */
        private boolean tryStart() {
            if (valueAnimator != null && !valueAnimator.isStarted()) {
                valueAnimator.start();
                return true;
            }
            return false;
        }

        /**
         * @return true if the contained value animator was ended by this action, otherwise false.
         * */
        private boolean tryEnd() {
            if (valueAnimator != null && valueAnimator.isStarted()) {
                valueAnimator.end();
                return true;
            }
            return false;
        }
    }
}
