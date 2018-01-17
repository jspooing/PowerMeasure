package com.example.js.myapplication;

/**
 * 블루투스 연결 기기에서 측정값을 받아 처리하는 클래스
 */


public class measureProcess {
    float[] measureValues;  //값을 저장할 int 배열
    int sizeValues;         //배열의 사이즈
    int lastPosition;       //배열에 저장된 마지막 값의 위치

    measureProcess(){
        //기본 생성자
        measureValues=new float[10];
        sizeValues = 10;
        lastPosition = 0;
    }

    measureProcess(int size){
        measureValues = new float[size];
        this.sizeValues = size;
        lastPosition = 0;
    }

    int getSize(){
        return sizeValues;
    }
    float getValueAt(int i){
        //i위치에 값 얻어오기
        return measureValues[i];
    }

    int getLastPosition(){
        return lastPosition;
    }
    float[] getValues(){
        float[] values = new float[sizeValues];
        for(int i=0;i < sizeValues;i++){
            values[i] = getValueAt(i);
        }
        return values;
    }

    void pushValue(String vStr){
        //새로운 값을 배열에 저장하기
        float v = Float.parseFloat(vStr);
        if(lastPosition == sizeValues){
            //배열이 꽉 차있을 경우 가장 오래된 값을 지우고 새로운 값을 배열의 뒤에 넣는다.
            for(int i=1; i < sizeValues; i ++){
                measureValues[i-1]=measureValues[i];
            }
            measureValues[sizeValues-1] = v;
        }
        else{
            //배열의 값을 저장하고 마지막 값을 하나 증가
            measureValues[lastPosition]= v;
            lastPosition++;
        }
    }
}

