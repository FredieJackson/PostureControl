#include <ESP8266WiFi.h>

#define CLIENT_DATA_REQUEST 0x504443
#define CLIENT_DATA_REQUEST_SIZE 3

const char* ap_ssid     = "PostureControl";
const char* ap_password = "123445678";

WiFiServer server(32015);
WiFiClient client;

void setup_wifi()
{ 
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ap_ssid, ap_password);
  
  //Start the server
  server.begin();
  Serial1.println("Server started");

  // Print the IP address
  Serial1.println(WiFi.localIP());
}

void setup() {
  Serial.begin(115200);
  Serial.swap();
  setup_wifi();
  delay(500);
}

bool check_client_connection(WiFiServer *server, WiFiClient *client)
{
  //if client has been connected
  if(*client)
  {
	  //if client is still connected 
	  if(client->status() != 0) 
	  {
	    return true;
	  }
	  //if client has disconected
	  else 
	  {
	    //Serial.println("Client disonnected");
		  *client = WiFiClient();
	  }
  }
  //If there is incoming client connection
  if(!server->hasClient())
  {
	  return false;
  }
  //connect it
  *client = server->available();
  //and print message
  Serial1.print("new client: ");
  Serial1.print(client->remoteIP());
  Serial1.print(":");
  Serial1.println(client->remotePort());
  return true;
}

#define MPU_COUNT 5
//4 quat componet 4 byte each for each mpu
#define IMU_DATA_SIZE (MPU_COUNT * 16)

byte imu_data[IMU_DATA_SIZE];

void wifi_proc()
{
  //if client is not connected or there is no incoming information
  if(!check_client_connection(&server, &client) || !client.available())
  {
	  return;
  }
  
  // Read the first line of the request
  uint32_t request = 0;
  client.read((uint8_t*)&request, CLIENT_DATA_REQUEST_SIZE);
  
  if(request != CLIENT_DATA_REQUEST)
  {
    client.flush();
    return;
  }
  
  // Send the response to the client
  client.write((byte*)&imu_data, IMU_DATA_SIZE);
}

#define DATA_ENQUIRY 0x5

void get_imu_data()
{
  Serial.write(DATA_ENQUIRY);
  Serial.readBytes(imu_data, IMU_DATA_SIZE);
}

void loop() 
{
  get_imu_data();
  wifi_proc();
}

