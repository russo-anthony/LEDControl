#include <project.h>
/*
Anthony Russo & Derek Arnheiter
ELC 343 - MicroComputer Systems
PSoC Firmware - BLE LED Control
 */
/***************************************************************
 * Function to update the Compare Value in the GATT database
 **************************************************************/
void updateCompare(uint8 val)
{
    CYBLE_GATTS_HANDLE_VALUE_NTF_T 	tempHandle;
    
    //If The BLE Device Is Not Connected Keep The PWM Compare Value At 0
    if(CyBle_GetState() != CYBLE_STATE_CONNECTED)
            controlPWM_WriteCompare(0);
        return;
    
    //Set The compareValue To The Give  Value If The Device Is Connected
    uint8 compareValue = val;
    
    //Using A Temporary Handle, Write The Value Into The Gatt Database
    tempHandle.attrHandle = CYBLE_LEDSERVICE_PWMCOMPARE_CHAR_HANDLE;
  	tempHandle.value.val = &compareValue;
    tempHandle.value.len = 1;
    CyBle_GattsWriteAttributeValue(&tempHandle,0,&cyBle_connHandle,CYBLE_GATT_DB_LOCALLY_INITIATED);  
}

/***************************************************************
 * Function to handle the BLE stack
 **************************************************************/
void BleCallBack(uint32 event, void* eventParam)
{
    CYBLE_GATTS_WRITE_REQ_PARAM_T *wrReqParam;

    switch(event)
    {
        /* if there is a disconnect or the stack just turned on from a reset then start the advertising and turn on the LED blinking */
        case CYBLE_EVT_STACK_ON:
        case CYBLE_EVT_GAP_DEVICE_DISCONNECTED:
            CyBle_GappStartAdvertisement(CYBLE_ADVERTISING_FAST);
            pwm_Start();
            blue_Write(1);
            updateCompare(0);
        break;
        
        /* when a connection is made, update the compare value in the GATT database and stop blinking the LED */    
        case CYBLE_EVT_GATT_CONNECT_IND:
            updateCompare(0);  
            pwm_Stop();
            blue_Write(0);  //Force Blue LED Off
		break;

        /* handle a write request */
        case CYBLE_EVT_GATTS_WRITE_REQ:
            wrReqParam = (CYBLE_GATTS_WRITE_REQ_PARAM_T *) eventParam;
			      
            /* request write the compareValue value */
            if(wrReqParam->handleValPair.attrHandle == CYBLE_LEDSERVICE_PWMCOMPARE_CHAR_HANDLE)
            {
                /* only update the value and write the response if the requested write is allowed */
                if(CYBLE_GATT_ERR_NONE == CyBle_GattsWriteAttributeValue(&wrReqParam->handleValPair, 0, &cyBle_connHandle, CYBLE_GATT_DB_PEER_INITIATED))
                {
                    controlPWM_WriteCompare(wrReqParam->handleValPair.value.val[0]);      //Update Compare Value
                    CyBle_GattsWriteRsp(cyBle_connHandle);
                }
            }		
		break;  
        
        default:
            break;
    }
} 

/***************************************************************
 * Main
 **************************************************************/
int main()
{
    CyGlobalIntEnable; 
    
    /* Start BLE stack and register the callback function */
    CyBle_Start(BleCallBack);
    
    //Start Clock 1 (Advertising PWM Clock)
    Clock_1_Start();
    
    //Start controlPWM and Clock 2 (controlPWM Clock)
    controlPWM_Start();
    Clock_2_Start();
    
    //Process Bluetooth Events
    for(;;)
    {        
        CyBle_ProcessEvents();
    }
}