package com.airy.my2048;

import static com.airy.my2048.GameBoardUtils.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_ID = 1;
    private GridView boardGrid;
    private Button startButton;
    private TextView scoreText;
    private BoardAdapter adapter;
    private MenuItem undoButton;
    private MenuItem restartButton;
    private boolean isNormalSize;
    private float startX, startY, offsetX, offsetY;
    private static final String TAG = "board";
    private static final String CHANNEL_ID = "game_over";
    private static final int DELAY_MILLIS = 300;
    private static final String SCORE_TAG = "score";
    private static final String BOARD_ARRAY_TAG = "board";

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boardGrid = findViewById(R.id.board_grid);
        startButton = findViewById(R.id.start_button);
        scoreText = findViewById(R.id.score_text);
        isNormalSize = getSupportActionBar()!=null;

        dataInit(savedInstanceState);

        startButton.setOnClickListener(v -> gameViewInit());

        boardGrid.setOnTouchListener((v, event) -> {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    offsetX = event.getX() - startX;
                    offsetY = event.getY() - startY;
                    int gestureKey = NONE_SWIPED;
                    int length = isNormalSize?120:45;
                    if(Math.abs(offsetX)>Math.abs(offsetY)){
                        if(offsetX > length){
                            gestureKey = RIGHT_WARD;
                        }else if(offsetX <-length){
                            gestureKey = LEFT_WARD;
                        }
                    }else{
                        if(offsetY > length){
                            gestureKey = DOWN_WARD;
                        }else if(offsetY < -length){
                            gestureKey = UP_WARD;
                        }
                    }
                    gameNextPace(gestureKey);
                    break;
            }
            return true;
        });
    }

    /**
     * 通过检查bundle是否为空以确认游戏是否在进行到一半是重新创建了activity（比如：切换了黑夜模式）
     * 如果bundle非空，则恢复游戏数据，包括2048表盘和分数
     * 如果bundle为空，则初始化游戏数据，并生成开始按钮
     *
     * @param savedInstanceState onCreate()中的bundle
     */
    private void dataInit(Bundle savedInstanceState) {
        if(savedInstanceState!=null){
            int score = savedInstanceState.getInt(SCORE_TAG);
            ArrayList<Integer> board = savedInstanceState.getIntegerArrayList(BOARD_ARRAY_TAG);
            restoreGameData(score,board);
            gameViewUpdate(board);
        }else{
            initGameData();
            startButtonInit();
        }
    }

    /**
     * 生成游戏开始按钮
     */
    private void startButtonInit() {
        boardGrid.setVisibility(View.GONE);
        startButton.setVisibility(View.VISIBLE);
        if(restartButton!=null)restartButton.setEnabled(false);
    }

    /**
     * 初始化游戏界面，包括移除开始按钮
     */
    private void gameViewInit(){
        boardGrid.setVisibility(View.VISIBLE);
        startButton.setVisibility(View.GONE);
        adapter = new BoardAdapter(getBoardInList(),this);
        boardGrid.setAdapter(adapter);
        if(undoButton!=null)undoButton.setEnabled(false);
        if(restartButton!=null)restartButton.setEnabled(true);
        setScore();
    }

    /**
     * 刷新游戏界面
     */
    private void gameViewUpdate(){
        adapter.updateList(getBoardInList());
        setScore();
    }

    /**
     * 刷新游戏界面
     * @param list 指定刷新的数据
     */
    private void gameViewUpdate(ArrayList<Integer> list){
        adapter.updateList(list);
        if(undoButton!=null)undoButton.setEnabled(false);
        setScore();
    }

    /**
     * 数据保存到bundle中。使得当activity被销毁并重新创建时能够还原2048表盘数据
     * @param outState 用于存储数据的bundle
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SCORE_TAG,getScore());
        outState.putIntegerArrayList(BOARD_ARRAY_TAG, getBoardInList());
    }

    /**
     * 每次成功触发滑动事件都会调用这个方法
     * 滑动后，会刷新游戏界面，指定需要设置动画的item以表示这是个新生成的方块，
     * 并判断游戏是否已经结束
     * @param key 手势
     */
    private void gameNextPace(int key){
        if(getIsGameOver())return;
        int locationKey = checkGesture(key);
        if(undoButton!=null && isUndoable())undoButton.setEnabled(true);
        if(getIsMove()){
            new Handler().postDelayed(() -> {
                adapter.setGeneratePosition(locationKey);
                gameViewUpdate();
            },DELAY_MILLIS);
            if(getIsGameOver()){
                gameOver();
            }
        }
    }

    /**
     * 游戏结束时会调用这个方法
     * 会发出游戏结束的通知，以及询问是否要继续游戏的AlertDialog
     */
    private void gameOver() {
        if(undoButton!=null)undoButton.setEnabled(false);
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setSmallIcon(R.drawable.abc_vector_test)
                .setContentTitle("Game Over!")
                .setContentText("max cube:"+getMaxCube())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0));
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(NOTIFICATION_ID,builder.build());

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Game Over...")
                .setMessage("restart the game?")
                .setPositiveButton("Yes", (dialog1, which) -> {
                    restartGame();
                    Log.d(TAG, "onClick: "+getIsGameOver());
                })
                .setNegativeButton("No", (dialog12, which) -> {
                    initGameData();
                    startButtonInit();
                }).show();
    }

    private void restartGame(){
        initGameData();
        gameViewUpdate();
        if(undoButton!=null)undoButton.setEnabled(false);
    }

    /**
     * 更新分数
     */
    @SuppressLint("SetTextI18n")
    private void setScore() {
        scoreText.setText("score: "+getScore());
    }

    /**
     * 创建NotificationChannel
     */
    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Game Over Notification",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("show notification when game finish");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * 生成actionBar按钮
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(isNormalSize){
            getMenuInflater().inflate(R.menu.main_menu,menu);
            undoButton = menu.findItem(R.id.action_game_undo);
            restartButton = menu.findItem(R.id.action_game_restart);
            undoButton.setEnabled(false);
            return true;
        }else return super.onCreateOptionsMenu(menu);
    }

    /**
     * 响应actionBar点击操作
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_game_restart:
                restartGame();
                return true;
            case R.id.action_game_undo:
                undoGameData();
                gameViewUpdate();
                undoButton.setEnabled(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}