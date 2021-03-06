package com.energer.freestylegame.controller;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.energer.freestylegame.R;

public class SlideAdapter extends PagerAdapter {
    Context context;
    LayoutInflater inflater;

    public SlideAdapter(Context context){
        this.context=context;
    }

    //Arrays
    public int[] slideImages={
            R.drawable.slide_1,
            R.drawable.slide_2,
            R.drawable.slide_3,
            R.drawable.slide_4,
    };

    public int[] slideTitle={
            R.string.slide_1_name,
            R.string.slide_2_name,
            R.string.slide_3_name,
            R.string.slide_4_name
    };

    public int[] slideText={
            R.string.slide_1_explication,
            R.string.slide_2_explication,
            R.string.slide_3_explication,
            R.string.slide_4_explication
    };

    @Override
    public int getCount() {
        return slideTitle.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view== object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view=inflater.inflate(R.layout.slide_layout,container,false);

        ImageView slideImg=(ImageView)view.findViewById(R.id.slide_img);
        TextView slideTitleView=(TextView)view.findViewById(R.id.slide_title);
        TextView slideTxt=(TextView)view.findViewById(R.id.slide_text);
        Button btn_continue=view.findViewById(R.id.slide_continue);
        View fleche=view.findViewById(R.id.slide_slide);

        slideImg.setImageResource(slideImages[position]);
        slideTitleView.setText(slideTitle[position]);
        slideTxt.setText(slideText[position]);

        if(position==3){
            btn_continue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    context.startActivity(new Intent(context,MainActivity.class));
                }
            });
            fleche.setVisibility(View.GONE);
            btn_continue.setVisibility(View.VISIBLE);
        }else{
            fleche.setVisibility(View.VISIBLE);
        }

        container.addView(view);

        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((LinearLayout)object);
    }
}
