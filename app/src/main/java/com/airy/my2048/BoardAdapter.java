package com.airy.my2048;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class BoardAdapter extends BaseAdapter {
    private List<Integer> boardList;
    private final Context context;
    private final Animation alphaAnimation = new AlphaAnimation(0,1);
    private int generatePosition = -1;
    private static final int DELAY_MILLIS = 200;
    private final int[] colorArray;
    private static final int[] boardNumber = new int[10];
    static {
        for (int i = 0; i < 10; i++) {
            boardNumber[i] = (int)Math.pow(2,i+1);
        }
    }

    public BoardAdapter(List<Integer> boardList, Context context) {
        this.boardList = boardList;
        this.context = context;
        alphaAnimation.setDuration(DELAY_MILLIS);
        colorArray = context.getResources().getIntArray(R.array.maple_color_set);
    }

    /**
     * 指定需要设置动画的item
     * @param generatePosition 随机生成数字的位置
     */
    public void setGeneratePosition(int generatePosition) {
        this.generatePosition = generatePosition;
    }

    @Override
    public int getCount() {
        return boardList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void updateList(List<Integer> boardList){
        this.boardList = boardList;
        notifyDataSetChanged();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.item2048,parent,false);
            holder = new ViewHolder();
            holder.itemNumText = convertView.findViewById(R.id.item_num);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }

        int num = boardList.get(position);
        String content;
        if(num!=0)content = Integer.toString(num);
        else content = "";
        holder.itemNumText.setText(content);
        holder.itemNumText.setBackgroundColor(getColor(num));

        if(position == generatePosition)convertView.startAnimation(alphaAnimation);
        else{}
        return convertView;
    }

    private static class ViewHolder{
        TextView itemNumText;
    }

    /**
     * 根据主题颜色数组和数字大小为方块获取背景颜色
     * @param num 当前item的数字
     * @return 颜色
     */
    public int getColor(int num){
        int color = Color.WHITE;
        if(num!=0){
            for (int i = 0; i < 10; i++) {
                if(num==boardNumber[i]){
                    color = colorArray[i];
                    break;
                }else if(i==9 && num>boardNumber[i]){
                    color = colorArray[colorArray.length-1];
                }
            }
        }
        return color;
    }
}
