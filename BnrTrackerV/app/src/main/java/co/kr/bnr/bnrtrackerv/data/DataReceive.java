package co.kr.bnr.bnrtrackerv.data;

import co.kr.bnr.bnrtrackerv.util.CommonUtil;

/**
 * 청소기가 계속 송출하고 있는 상태 정보
 * 7E 7E 02 9a 0a 3d 0a 1a 00 c3 10 00 00 0d 48 03
 */

public class DataReceive {
    public String opCode;//Op code가 2가지로 출력되며, op code에 따라서 표현하는 내용 차이가 발생함.
    public String magnetSettingState;// 앞 1자리 : 설정 상태 표현 (0x : 기본 상태, 1x : 각도 설정 모드, 2x : 각도 수평 설정 모드, 3x : 수직 설정 모드, 4x : 얇은 유리 설정 모드, 5x : 두꺼운 유리 설정 모드)
    public String model;//모델명 (x0 : 설정 안됨, x1 : RT10, x2 : RT16, x3 : RT22, x4 : RT28)
    public double batteryVoltage;// 배터리전압표현:(수신된데이터+500)/100으로전압표현 (표현범위6.0~8.5V)
    public String magnetLeft;// 왼쪽 자력값 0~4096표현(4자리가MSB, 5자리가LSB)
    public String magnetRight;// 오른쪽 자력값 0~4096표현(4자리가MSB, 5자리가LSB)
    public String magnetAverage;// 자력 초기 설정 값으로 (수신데이터 + 150)으로 출력
    public String magnetCenter;// 유리 두께 측정 후 평균 값으로 (수신데이터 + 150)으로 출력
    public String magnetRange;// 두께 범위 값으로 수신 데이터로 출력
    public String bumperState;// 범퍼 인식 상태 표현, 범퍼가 눌려지면 눌려진 부분의 색상 표현 (앞 1자리는 Log가 위치한 방향의 범퍼 표현, 뒤 1자리는 손잡이가 위치한 범퍼 인식 표현(우측 이미지 참조)
    public String ultrasonicLeft;// 왼쪽 초음파 센서 거리 표현 (범위 0.0 ~ 1.50 m), (수신 데이터 / 100) 표현
    public String ultrasonicRight;// 오른쪽 초음파 센서 거리 표현 (범위 0.0 ~ 1.50 m), (수신 데이터 / 100) 표현

    //앞에 7E7E로 시작핳고 7D7D로 끝난다
    public DataReceive(String[] arrSplitedRawdata) {
        this.opCode = arrSplitedRawdata[1];

        if(arrSplitedRawdata[2].startsWith("0"))
            this.magnetSettingState = "기본상태";
        else if(arrSplitedRawdata[2].startsWith("1"))
            this.magnetSettingState = "각도 설정 모드";
        else if(arrSplitedRawdata[2].startsWith("2"))
            this.magnetSettingState = "각도 수평 설정 모드";
        else if(arrSplitedRawdata[2].startsWith("3"))
            this.magnetSettingState = "수직 설정 모드";
        else if(arrSplitedRawdata[2].startsWith("4"))
            this.magnetSettingState = "얇은 유리 설정 모드";
        else if(arrSplitedRawdata[2].startsWith("5"))
            this.magnetSettingState = "두꺼운 유리 설정 모드";

        if(arrSplitedRawdata[2].endsWith("0"))
            this.model = "설정 안됨";
        else if(arrSplitedRawdata[2].endsWith("1"))
            this.model = "RT10";
        else if(arrSplitedRawdata[2].endsWith("2"))
            this.model = "RT16";
        else if(arrSplitedRawdata[2].endsWith("3"))
            this.model = "RT22";
        else if(arrSplitedRawdata[2].endsWith("4"))
            this.model = "RT28";

        this.batteryVoltage = ((double)CommonUtil.hexToDecimal(arrSplitedRawdata[3]) + 500) / 100;
        this.magnetLeft = String.valueOf((CommonUtil.hexToDecimal(arrSplitedRawdata[4] + arrSplitedRawdata[5])));
        this.magnetRight = String.valueOf((CommonUtil.hexToDecimal(arrSplitedRawdata[6] + arrSplitedRawdata[7])));
        this.magnetAverage = String.valueOf(CommonUtil.hexToDecimal(arrSplitedRawdata[8]) + 150);
        this.magnetCenter = String.valueOf(CommonUtil.hexToDecimal(arrSplitedRawdata[9]) + 150);
        this.magnetRange = String.valueOf(CommonUtil.hexToDecimal(arrSplitedRawdata[10]));
        this.bumperState = String.valueOf(CommonUtil.hexToDecimal(arrSplitedRawdata[11]));//이거 모르겠어요 ㅠ
        this.ultrasonicLeft = String.valueOf(CommonUtil.hexToDecimal(arrSplitedRawdata[12]) /100);
        this.ultrasonicRight = String.valueOf(CommonUtil.hexToDecimal(arrSplitedRawdata[13]) /100);
    }
}
