package com.github.troy.animatedlayerview;

import android.animation.ValueAnimator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.troy.animatedlayers.AnimatedLayerView;
import com.github.troy.animatedlayers.LayerConfig;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private AnimatedLayerView mStadiumCover;
    private AnimatedLayerView mAnimatedLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mStadiumCover = findViewById(R.id.animated_cover);
        mAnimatedLogo = findViewById(R.id.animated_logo);

        initStadiumCover();

        initAnimatedLogo();
    }

    private void initStadiumCover() {
        ArrayList<LayerConfig> layerConfigs = new ArrayList<>();
        layerConfigs.add((new LayerConfig.Builder(R.drawable.rippled_layer_1, AnimatedLayerView.TRANSLATE_START))
                .duration(20000).layerGravity(AnimatedLayerView.ALIGN_BOTTOM).build());
        layerConfigs.add((new LayerConfig.Builder(R.drawable.rippled_layer_2, AnimatedLayerView.TRANSLATE_START))
                .duration(20000).layerGravity(AnimatedLayerView.ALIGN_BOTTOM).build());
        layerConfigs.add((new LayerConfig.Builder(R.drawable.rippled_layer_3, AnimatedLayerView.TRANSLATE_START))
                .duration(20000).layerGravity(AnimatedLayerView.ALIGN_BOTTOM).build());
        mStadiumCover.addLayerList(layerConfigs);
    }

    private void initAnimatedLogo() {
        ArrayList<LayerConfig> layerConfigs = new ArrayList<>();
        layerConfigs.add((new LayerConfig.Builder(R.drawable.bvb_logo, AnimatedLayerView.ROTATE_CLOCKWISE))
                .layerGravity(AnimatedLayerView.CENTER).layerScaleType(AnimatedLayerView.CENTER_INSIDE).duration(3000).build());
        layerConfigs.add((new LayerConfig.Builder(R.drawable.soccer, AnimatedLayerView.TRANSLATE_UP))
                .duration(1000).animationInterval(getResources().getDimension(R.dimen.soccer_translate_interval))
                .layerGravity(AnimatedLayerView.ALIGN_BOTTOM | AnimatedLayerView.CENTER_HORIZONTAL)
                .margin(0, 0, 0, 5).repeatMode(ValueAnimator.REVERSE).build());
        layerConfigs.add((new LayerConfig.Builder(R.drawable.soccer, AnimatedLayerView.TRANSLATE_END))
                .duration(1000).animationInterval(getResources().getDimension(R.dimen.soccer_translate_interval))
                .layerGravity(AnimatedLayerView.ALIGN_START | AnimatedLayerView.CENTER_VERTICAL)
                .margin(5, 0, 0, 0).repeatMode(ValueAnimator.REVERSE).build());
        mAnimatedLogo.replaceLayerList(layerConfigs);
    }
}
