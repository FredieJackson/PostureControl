// I2Cdev and MPU6050 must be installed as libraries, or else the .cpp/.h files
// for both classes must be in the include path of your project
#include "I2Cdev.h"

#include "MPU6050_6Axis_MotionApps20.h"

#include "Wire.h"

#define MPU_COUNT   5
#define MPU_ADDRESS 0x68
MPU6050 mpu[MPU_COUNT];

// MPU control/status vars
bool     dmpReady = false;  // set true if MPU init was successful
uint8_t  mpuIntStatus;      // holds actual interrupt status byte from MPU
uint16_t packetSize;        // expected DMP packet size (default is 42 bytes)
uint16_t fifoCount;         // count of all bytes currently in FIFO
uint8_t  fifoBuffer[64];    // FIFO storage buffer
uint16_t mpu_offsets[][6] = 
{
  {-1207, -4369, 983, 83, -3, 21},
  {907, -734, 858, 102, -25, 56},
  {139, -3505, 1506, 151, 66, -51},
  {-1517, -1129, 1647, 134, -12, -107},
  {-4132, -493, 613, 90, -61, 43}
};
// quats
Quaternion q[MPU_COUNT]; // [w, x, y, z]

#define SCL_PIN 19//4
#define SDA_PIN 18//5
#define MULTIPLEXOR_A_PIN 2//16
#define MULTIPLEXOR_B_PIN 3//0
#define MULTIPLEXOR_C_PIN 4//2
#define OVERFLOW_LED_PIN  5

void select_mpu(uint8_t num)
{
  digitalWrite(MULTIPLEXOR_A_PIN, num       & 0x1);
  digitalWrite(MULTIPLEXOR_B_PIN,(num >> 1) & 0x1);
  digitalWrite(MULTIPLEXOR_C_PIN,(num >> 2) & 0x1);
  delayMicroseconds(4);
}

void mpu_setup(uint8_t mpu_num)
{
  int reconnectCount = 0;
  int reconnectMax = 5;
  int devStatus;
  select_mpu(mpu_num);
  // initialize device
  tryAgain:
  mpu[mpu_num].initialize();
  // verify connection
  if(!mpu[mpu_num].testConnection())
  {
    goto reconnect;
  }
  delayMicroseconds(50);
  // load and configure the DMP
  devStatus = mpu[mpu_num].dmpInitialize();

  mpu[mpu_num].setXAccelOffset(mpu_offsets[mpu_num][0]);
  mpu[mpu_num].setYAccelOffset(mpu_offsets[mpu_num][1]);
  mpu[mpu_num].setZAccelOffset(mpu_offsets[mpu_num][2]);
  mpu[mpu_num].setXGyroOffset (mpu_offsets[mpu_num][3]);
  mpu[mpu_num].setYGyroOffset (mpu_offsets[mpu_num][4]);
  mpu[mpu_num].setZGyroOffset (mpu_offsets[mpu_num][5]);

  // make sure it worked (returns 0 if so)
  if (devStatus == 0)
  {
    // turn on the DMP
    mpu[mpu_num].setDMPEnabled(true);

    // set our DMP Ready flag so the main loop() function knows it's okay to use it
    dmpReady = true;

    // get expected DMP packet size
    packetSize = mpu[mpu_num].dmpGetFIFOPacketSize();
  } 
  else
  {
    reconnect:
    if(reconnectCount < reconnectMax)
    {
      digitalWrite(OVERFLOW_LED_PIN, HIGH);
      delay(10);
      digitalWrite(OVERFLOW_LED_PIN, LOW);
      reconnectCount++;
      goto tryAgain;
    }
    dmpReady = false;
  }
}

void setup(void)
{
  pinMode(MULTIPLEXOR_A_PIN, OUTPUT);
  pinMode(MULTIPLEXOR_B_PIN, OUTPUT);
  pinMode(MULTIPLEXOR_C_PIN, OUTPUT);
  pinMode(OVERFLOW_LED_PIN, OUTPUT);
  Wire.setClock(400000);
  Wire.begin(); 
  delay(500);
  Serial.begin(115200);
  for (int i = 0; i < MPU_COUNT; i++)
  {
    mpu_setup(i);
    if (!dmpReady)
    {
      digitalWrite(OVERFLOW_LED_PIN, HIGH);
      break;    
    }
  }
}

typedef union 
{
  float value;
  byte  bytes[sizeof(float)];
} binary_float;

int8_t get_mpu_quat(uint8_t mpu_num)
{
  // if mpu weren't initialized
  if (!dmpReady)
  {
    return -1;
  }
  
  //select mpu by multiplexor
  select_mpu(mpu_num);

  //get int status
  mpuIntStatus = mpu[mpu_num].getIntStatus();
  
  // get current FIFO count
  fifoCount = mpu[mpu_num].getFIFOCount();

  // check for FIFO overflow
  if ((mpuIntStatus & 0x10) || fifoCount == 1024)
  {
    // reset so we can continue cleanly
    mpu[mpu_num].resetFIFO();
    digitalWrite(OVERFLOW_LED_PIN, HIGH);
    return -1; 
  }
  // otherwise, check for DMP data ready interrupt
  if (mpuIntStatus & 0x02)
  {
    digitalWrite(OVERFLOW_LED_PIN, LOW);
    // wait for correct available data length, should be a VERY short wait
    while (fifoCount < packetSize) 
    {
      fifoCount = mpu[mpu_num].getFIFOCount();
    }

    // read a packet from FIFO
    mpu[mpu_num].getFIFOBytes(fifoBuffer, packetSize);

    mpu[mpu_num].dmpGetQuaternion(&q[mpu_num], fifoBuffer);
    return 0;
  }
  return -1;
}

binary_float tmp;
void send_quat_to_uart(Quaternion *quat)
{
    tmp.value = quat->w;
    Serial.write(tmp.bytes, sizeof(float));
    tmp.value = quat->x;
    Serial.write(tmp.bytes, sizeof(float));
    tmp.value = quat->y;
    Serial.write(tmp.bytes, sizeof(float));
    tmp.value = quat->z;
    Serial.write(tmp.bytes, sizeof(float));
}

#define DATA_ENQUIRY 0x5

uint8_t check_data_enquiry()
{
  //is there are no data in serial then return 0
  if(!Serial.available())
  {
    return 0;
  }
  //read 1 byte.
  char buff = Serial.read();
  //if it is a data request
  if(buff == DATA_ENQUIRY)
  {
    return 1;
  }
  return 0;
}

void loop(void)
{
  for (uint8_t i = 0; i < MPU_COUNT; i++)
  {
    get_mpu_quat(i);
  }
  //if there is a data enquiry
  if(check_data_enquiry())
  {
	//send data to uart
    for (uint8_t i = 0; i < MPU_COUNT; i++)
    {
      send_quat_to_uart(&q[i]);
    }
  }
}
