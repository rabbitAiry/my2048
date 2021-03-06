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
     * ????????????bundle?????????????????????????????????????????????????????????????????????activity????????????????????????????????????
     * ??????bundle???????????????????????????????????????2048???????????????
     * ??????bundle?????????????????????????????????????????????????????????
     *
     * @param savedInstanceState onCreate()??????bundle
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
     * ????????????????????????
     */
    private void startButtonInit() {
        boardGrid.setVisibility(View.GONE);
        startButton.setVisibility(View.VISIBLE);
        if(restartButton!=null)restartButton.setEnabled(false);
    }

    /**
     * ????????????????????????????????????????????????
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
     * ??????????????????
     */
    private void gameViewUpdate(){
        adapter.updateList(getBoardInList());
        setScore();
    }

    /**
     * ??????????????????
     * @param list ?????????????????????
     */
    private void gameViewUpdate(ArrayList<Integer> list){
        adapter.updateList(list);
        if(undoButton!=null)undoButton.setEnabled(false);
        setScore();
    }

    /**
     * ???????????????bundle???????????????activity???????????????????????????????????????2048????????????
     * @param outState ?????????????????????bundle
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SCORE_TAG,getScore());
        outState.putIntegerArrayList(BOARD_ARRAY_TAG, getBoardInList());
    }

    /**
     * ??????????????????????????????????????????????????????
     * ???????????????????????????????????????????????????????????????item???????????????????????????????????????
     * ?????????????????????????????????
     * @param key ??????
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
     * ????????????????????????????????????
     * ?????????????????????????????????????????????????????????????????????AlertDialog
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
     * ????????????
     */
    @SuppressLint("SetTextI18n")
    private void setScore() {
        scoreText.setText("score: "+getScore());
    }

    /**
     * ??????NotificationChannel
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
     * ??????actionBar??????
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
     * ??????actionBar????????????
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