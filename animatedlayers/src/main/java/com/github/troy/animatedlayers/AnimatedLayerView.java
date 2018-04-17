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
import android.graphics.drawable.ShapeDrawable;
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
    public static final int SCALE = 7; //Type of SCALE only supports repeatMode == REVERSE

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
                info.layerShader.setLocalMatrix(info.getMatrix());
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

    private void layoutLayer(final Layer info, float extraSpaceXPercentageForScale, float extraSpaceYPercentageForScale) {
        //Config Matrix and target Rect to draw
        info.targetRect = new Rect(0, 0, vWidth, vHeight);
        if (info.layerGravity == CENTER) {
            int widthOffset = (vWidth - info.drawableWidth)/2;
            int heightOffset = (vHeight - info.drawableHeight)/2;
            if (info.animationType == ROTATE_CLOCKWISE || info.animationType == ROTATE_ANTICLOCKWISE) {
                int rectX = vWidth - info.drawableWidth > 0 ? (vWidth - info.drawableWidth)/2 : 0;
                int rectY = vHeight - info.drawableHeight > 0 ? (vHeight - info.drawableHeight)/2 : 0;
                info.targetRect = new Rect(rectX, rectY, vWidth - rectX, vHeight - rectY);
            }
            float scaleX = vWidth / (info.drawableWidth*1f);
            float scaleY = vHeight / (info.drawableHeight*1f);
            //Only scale down, no scale up for gravity == CENTER
            if (info.layerScaleType == NO_SCALE) {
                info.translateX = widthOffset;
                info.translateY = heightOffset;
            } else if (info.layerScaleType == FITXY) {
                info.scaleX = Math.min(1, scaleX);
                info.scaleY = Math.min(1, scaleY);
                info.translateX = widthOffset;
                info.translateY = heightOffset;
            } else {
                float actualScaleX = Math.min(1, scaleX);
                float actualScaleY = Math.min(1, scaleY);
                float actualScale = info.layerScaleType == CENTER_INSIDE ?
                        Math.min(actualScaleX, actualScaleY) : Math.max(actualScaleX, actualScaleY);
                info.scaleX = actualScale;
                info.scaleY = actualScale;
                info.translateX = widthOffset;
                info.translateY = heightOffset;
            }
        } else if (info.layerGravity == FILL_PARENT){
            if(info.layerScaleType != NO_SCALE) {
                float scaleX = vWidth / (info.drawableWidth*1f);
                float scaleY = vHeight / (info.drawableHeight*1f);
                if (info.layerScaleType == FITXY) {
                    info.scaleX = scaleX;
                    info.scaleY = scaleY;
                } else {
                    float actualScale = info.layerScaleType == CENTER_INSIDE ?
                            Math.min(scaleX, scaleY) : Math.max(scaleX, scaleY);
                    info.scaleX = actualScale;
                    info.scaleY = actualScale;
                }
            }
        } else {
            if ((info.layerGravity & CENTER_HORIZONTAL) == CENTER_HORIZONTAL) {
                int widthOffset = vWidth - info.drawableWidth > 0 ? (vWidth - info.drawableWidth)/2 : 0;
                info.targetRect.top = info.marginTop;
                info.translateX += widthOffset;
            } else if ((info.layerGravity & CENTER_VERTICAL) == CENTER_VERTICAL) {
                int heightOffset = vHeight - info.drawableHeight > 0 ? (vHeight - info.drawableHeight)/2 : 0;
                info.targetRect.left = info.marginStart;
                info.translateY += heightOffset;
            }
            if ((info.layerGravity & ALIGN_TOP) == ALIGN_TOP) {
                if (info.animationType != TRANSLATE_UP && info.animationType != TRANSLATE_DOWN) {
                    info.targetRect.top = info.marginTop;
                    info.targetRect.bottom = (int) Math.min(info.drawableHeight*extraSpaceYPercentageForScale + info.marginTop, vHeight);
                }
                info.translateY += info.marginTop;
            } else if ((info.layerGravity & ALIGN_BOTTOM) == ALIGN_BOTTOM) {
                int rectOffset = vHeight - info.drawableHeight*extraSpaceYPercentageForScale > 0 ?
                        (int) (vHeight - info.drawableHeight*extraSpaceYPercentageForScale) : 0;
                if (info.animationType != TRANSLATE_UP && info.animationType != TRANSLATE_DOWN) {
                    info.targetRect.top = rectOffset - info.marginBottom;
                    info.targetRect.bottom = vHeight - info.marginBottom;
                }
                int transOffset = vHeight - info.drawableHeight > 0 ? vHeight - info.drawableHeight : 0;
                info.translateY += transOffset - info.marginBottom;
            }
            if ((info.layerGravity & ALIGN_START) == ALIGN_START) {
                if (info.animationType != TRANSLATE_START && info.animationType != TRANSLATE_END) {
                    info.targetRect.left = info.marginStart;
                    info.targetRect.right = (int) Math.min(info.drawableWidth*extraSpaceXPercentageForScale + info.marginStart, vWidth);
                }
                info.translateX += info.marginStart;
            } else if ((info.layerGravity & ALIGN_END) == ALIGN_END) {
                int rectOffset = vWidth - info.drawableWidth*extraSpaceXPercentageForScale > 0 ?
                        (int) (vWidth - info.drawableWidth*extraSpaceXPercentageForScale) : 0;
                if (info.animationType != TRANSLATE_START && info.animationType != TRANSLATE_END) {
                    info.targetRect.left = rectOffset - info.marginEnd;
                    info.targetRect.right = vWidth - info.marginEnd;
                }
                int transOffset = vWidth - info.drawableWidth > 0 ? vWidth - info.drawableWidth : 0;
                info.translateX += transOffset - info.marginEnd;
            }
        }
    }

    private void configLayerAnimator(final Layer info) {
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
                            System.out.println(animation.getAnimatedValue());
                            if (info.animationType == SCALE) {
                                if (fraction < lastFraction) {
                                    info.scaleX = info.toScaleX + info.baseScaleX * ((Float) animation.getAnimatedValue() - (info.fromValue + info.animationInterval));
                                    info.scaleY = info.toScaleY + info.baseScaleY * ((Float) animation.getAnimatedValue() - (info.fromValue + info.animationInterval));
                                } else {
                                    info.scaleX = info.fromScaleX + info.baseScaleX * ((Float) animation.getAnimatedValue() - info.fromValue);
                                    info.scaleY = info.fromScaleY + info.baseScaleY * ((Float) animation.getAnimatedValue() - info.fromValue);
                                }
                            }
                        }
                        lastFraction = fraction;

                        if (info.animationType == TRANSLATE_START) {
                            info.translateX -= change;
                        } else if (info.animationType == TRANSLATE_END) {
                            info.translateX += change;
                        } else if (info.animationType == TRANSLATE_UP) {
                            info.translateY -= change;
                        } else if (info.animationType == TRANSLATE_DOWN) {
                            info.translateY += change;
                        } else if (info.animationType == ROTATE_CLOCKWISE) {
                            info.rotateDegree += change;
                        } else if (info.animationType == ROTATE_ANTICLOCKWISE) {
                            info.rotateDegree -= change;
                        }

                        invalidate();
                    }
                });
                info.valueAnimator.start();
            }
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
        //Set initial value
        float extraSpaceXPercentageForScale = 1f;
        float extraSpaceYPercentageForScale = 1f;
        if (info.animationType == ROTATE_CLOCKWISE || info.animationType == ROTATE_ANTICLOCKWISE) {
            if (info.fromValue != 0) {
                info.rotateDegree += info.fromValue;
            }
        } else if (info.animationType == TRANSLATE_START || info.animationType == TRANSLATE_END) {
            if (info.fromValue != 0) {
                info.translateX += info.fromValue;
            }
        } else if (info.animationType == TRANSLATE_UP || info.animationType == TRANSLATE_DOWN) {
            if (info.fromValue != 0) {
                info.translateY += info.fromValue;
            }
        } else if (info.animationType == SCALE) {
            info.baseScaleX = info.scaleX;
            info.baseScaleY = info.scaleY;
            info.fromScaleX = info.scaleX * info.fromValue;
            info.fromScaleY = info.scaleY * info.fromValue;
            info.toScaleX = info.scaleX * (info.fromValue + info.animationInterval);
            info.toScaleY = info.scaleY * (info.fromValue + info.animationInterval);
            extraSpaceXPercentageForScale = Math.max(1, Math.max(info.fromScaleX, info.toScaleX));
            extraSpaceYPercentageForScale = Math.max(1, Math.max(info.fromScaleY, info.toScaleY));
        }

        layoutLayer(info, extraSpaceXPercentageForScale, extraSpaceYPercentageForScale);

        configLayerAnimator(info);

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
        Rect targetRect;
        ValueAnimator valueAnimator;
        float animationInterval;
        float fromValue;
        int duration;
        int repeatMode = ValueAnimator.RESTART;
        int repeatCount = ValueAnimator.INFINITE;
        TimeInterpolator interpolator = null;
        boolean configured = false;

        Matrix matrix;
        float translateX = 0f;
        float translateY = 0f;
        float rotateDegree = 0f;
        float scaleX = 1f;
        float scaleY = 1f;
        float baseScaleX = 1f;
        float baseScaleY = 1f;
        float fromScaleX = 1f;
        float fromScaleY = 1f;
        float toScaleX = 1f;
        float toScaleY = 1f;
        float scalePivotX = -1f;
        float scalePivotY = -1f;


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
            info.scalePivotX = config.getScalePivotX();
            info.scalePivotY = config.getScalePivotY();
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

        private Matrix getMatrix() {
            matrix.reset();
            matrix.setTranslate(translateX, translateY);
            float pivotX = scalePivotX == -1 ? targetRect.centerX() : targetRect.left + targetRect.width()*scalePivotX;
            float pivotY = scalePivotY == -1 ? targetRect.centerY() : targetRect.top + targetRect.height()*scalePivotY;
            matrix.postScale(scaleX, scaleY, pivotX, pivotY);
            matrix.postRotate(rotateDegree, targetRect.centerX(), targetRect.centerY());
            return matrix;
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
