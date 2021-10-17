package com.airy.my2048;

import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

public class GameBoardUtils {
    public static final int NONE_SWIPED = 0;
    public static final int UP_WARD = 1;
    public static final int DOWN_WARD = 2;
    public static final int LEFT_WARD = 3;
    public static final int RIGHT_WARD = 4;

    private static final int[][] board = new int[4][4];
    private static final ArrayList<Integer> emptyList = new ArrayList<>();
    private static boolean isGameRunning = false;
    private static boolean isMove = false;
    private static int maxCube = 0;
    private static int score = 0;
    private static final String TAG = "board";
    private static int formerScore;
    private static ArrayList<Integer> formerBoardList;

    /**
     * 使用单个数字来表示第i行第j列的位置
     */
    private static int getKeyViaLocation(int i, int j){
        return i*4+j;
    }

    /**
     * 从数字中解析这是哪一行哪一列
     */
    private static int[] getLocationViaKey(int key){
        return new int[]{key/4,key%4};
    }

    /**
     * 将第i行第j列设为空，并加入到记录空值的队列中
     */
    private static void setEmpty(int i, int j){
        emptyList.add(getKeyViaLocation(i, j));
        board[i][j] = 0;
        isMove = true;
    }

    /**
     * @return 游戏是否结束了
     */
    public static boolean getIsGameOver() {
        return !isGameRunning;
    }

    /**
     * @return 将表盘数据以列表形式返回
     */
    public static ArrayList<Integer> getBoardInList() {
        ArrayList<Integer> list = new ArrayList<>(16);
        for (int[] array :board) {
            for(int data:array){
                list.add(data);
            }
        }
        return list;
    }

    /**
     * @return 获取表盘中的最大值
     */
    public static int getMaxCube() {
        return maxCube;
    }

    /**
     * @return 获取当前分数
     */
    public static int getScore() {
        return score;
    }

    /**
     * @return 获取当前操作是否改变了表盘数据
     */
    public static boolean getIsMove() {
        return isMove;
    }

    /**
     * @return 是否能撤销操作
     */
    public static boolean isUndoable(){
        return formerBoardList!=null;
    }

    /**
     * 初始化游戏数据
     */
    public static void initGameData(){
        isGameRunning = true;
        maxCube = 0;
        score = 0;
        for (int i = 0; i < 16; i++) {
            int[] location = getLocationViaKey(i);
            board[location[0]][location[1]] = 0;
            emptyList.add(i);
        }
        randomGenerateNumber();
        randomGenerateNumber();
    }

    /**
     * 恢复游戏数据
     * 在从bundle中获得数据后或者撤销后调用
     * @param score 之前的分数
     * @param boardList 之前的表盘数据
     */
    public static void restoreGameData(int score, ArrayList<Integer> boardList){
        isGameRunning = true;
        GameBoardUtils.score = score;
        Log.d(TAG, "restoreGameBoard: "+boardList.size());
        for (int i = 0; i < 16; i++) {
            int[] location = getLocationViaKey(i);
            int value = boardList.get(i);
            board[location[0]][location[1]] = value;
            if(value == 0)emptyList.add(i);
            maxCube = Math.max(value,maxCube);
        }
    }

    /**
     * 检查游戏是否结束了
     */
    private static void checkIsFinished(){
        if(emptyList.size()==0){
            boolean isAllFilled = true;
            for (int i = 1; i < 4 && isAllFilled; i++) {
                for (int j = 0; j < 4 && isAllFilled; j++) {
                    if(board[i][j]==board[i-1][j])isAllFilled = false;
                }
            }
            for (int i = 0; i < 4 && isAllFilled; i++) {
                for (int j = 1; j < 4 && isAllFilled; j++) {
                    if(board[i][j]==board[i][j-1])isAllFilled = false;
                }
            }
            if(isAllFilled){
                isGameRunning = false;
            }
        }
    }

    /**
     * 表盘中随机生成2或者4
     * @return 生成数据的位置
     */
    private static int randomGenerateNumber(){
        if(emptyList.size()==0)return -1;
        Random random = new Random();
        int targetId = random.nextInt(emptyList.size());
        int value = random.nextInt(10)<1?4:2;
        int target = emptyList.get(targetId);
        int[] location = getLocationViaKey(target);
        board[location[0]][location[1]] = value;
        emptyList.remove(targetId);
        return target;
    }

    /**
     * 统计方块最大值和分数的过程中
     * @param i 第i行
     * @param j 第j列
     */
    private static void freshScore(int i, int j) {
        maxCube = Math.max(board[i][j],maxCube);
        score += board[i][j]/2;
    }

    /**
     * 逐个方块检查纵向滑动
     * @param i 第i行
     * @param j 第j列
     * @param toward 向上为1，向下为-1
     */
    private static void verticalIteration(int i, int j, int toward){
        int tempi = i;
        tempi -= toward;
        int gameBorder = toward==1?0:3;
        while(tempi>=0&&tempi<=3){
            if(board[tempi][j]==board[i][j]){
                board[tempi][j] *= 2;
                freshScore(tempi, j);
                setEmpty(i, j);
                break;
            }else if(board[tempi][j]!=0){
                tempi += toward;
                if(tempi != i){
                    board[tempi][j] = board[i][j];
                    emptyList.remove((Integer) getKeyViaLocation(tempi, j));
                    setEmpty(i, j);
                }
                break;
            }else if(tempi == gameBorder){
                board[tempi][j] = board[i][j];
                emptyList.remove((Integer) getKeyViaLocation(tempi, j));
                setEmpty(i, j);
            }
            tempi -= toward;
        }
    }

    /**
     * 逐个方块检查横向滑动
     * @param i 第i行
     * @param j 第j列
     * @param toward 向左为1，向右为-1
     */
    private static void horizontalIteration(int i, int j, int toward){
        int tempj = j;
        tempj -= toward;
        int gameBorder = toward==1?0:3;
        while(tempj>=0&&tempj<=3){
            if(board[i][tempj]==board[i][j]){
                board[i][tempj] *= 2;
                setEmpty(i, j);
                freshScore(i,tempj);
                break;
            }else if(board[i][tempj]!=0){
                tempj += toward;
                if(tempj != j){
                    board[i][tempj] = board[i][j];
                    emptyList.remove((Integer) getKeyViaLocation(i, tempj));
                    setEmpty(i, j);
                }
                break;
            }else if(tempj == gameBorder){
                board[i][tempj] = board[i][j];
                emptyList.remove((Integer) getKeyViaLocation(i, tempj));
                setEmpty(i, j);
            }
            tempj -= toward;
        }
    }

    /**
     * 上划
     */
    private static void upSwing(){
        for (int i = 1; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if(board[i][j]>0){
                    verticalIteration(i, j, 1);
                }
            }
        }
    }

    /**
     * 下划
     */
    private static void downSwing(){
        for (int i = 3; i >= 0; i--) {
            for (int j = 0; j < 4; j++) {
                if(board[i][j]>0){
                    verticalIteration(i, j, -1);
                }
            }
        }
    }

    /**
     * 左划
     */
    private static void leftSwing(){
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 4; i++) {
                if(board[i][j]>0){
                    horizontalIteration(i, j, 1);
                }
            }
        }
    }

    /**
     * 右划
     */
    private static void rightSwing(){
        for (int j = 3; j >= 0; j--) {
            for (int i = 0; i < 4; i++) {
                if(board[i][j]>0){
                    horizontalIteration(i, j, -1);
                }
            }
        }
    }

    /**
     * 根据手势执行相应滑动操作，并为撤销操作提供数据
     * @param key 滑动方向
     * @return 随机生成数字的位置
     */
    public static int checkGesture(int key){
        isMove = false;
        int positionKey = -1;
        int tempScore = score;
        ArrayList<Integer> nowBoardList = getBoardInList();
        switch (key) {
            case 1:
                upSwing();
                break;
            case 2:
                downSwing();
                break;
            case 3:
                leftSwing();
                break;
            case 4:
                rightSwing();
                break;
            default:
                break;
        }
        if(isMove){
            positionKey = randomGenerateNumber();
            checkIsFinished();
            formerScore = tempScore;
            formerBoardList = nowBoardList;
        }
        return positionKey;
    }

    /**
     * 撤销数据
     */
    public static void undoGameData(){
        if(isUndoable())restoreGameData(formerScore,formerBoardList);
        formerBoardList = null;
    }
}