/*******************************************************************************
* File Name: controlPWM_PM.c
* Version 3.30
*
* Description:
*  This file provides the power management source code to API for the
*  PWM.
*
* Note:
*
********************************************************************************
* Copyright 2008-2014, Cypress Semiconductor Corporation.  All rights reserved.
* You may use this file only in accordance with the license, terms, conditions,
* disclaimers, and limitations in the end user license agreement accompanying
* the software package with which this file was provided.
*******************************************************************************/

#include "controlPWM.h"

static controlPWM_backupStruct controlPWM_backup;


/*******************************************************************************
* Function Name: controlPWM_SaveConfig
********************************************************************************
*
* Summary:
*  Saves the current user configuration of the component.
*
* Parameters:
*  None
*
* Return:
*  None
*
* Global variables:
*  controlPWM_backup:  Variables of this global structure are modified to
*  store the values of non retention configuration registers when Sleep() API is
*  called.
*
*******************************************************************************/
void controlPWM_SaveConfig(void) 
{

    #if(!controlPWM_UsingFixedFunction)
        #if(!controlPWM_PWMModeIsCenterAligned)
            controlPWM_backup.PWMPeriod = controlPWM_ReadPeriod();
        #endif /* (!controlPWM_PWMModeIsCenterAligned) */
        controlPWM_backup.PWMUdb = controlPWM_ReadCounter();
        #if (controlPWM_UseStatus)
            controlPWM_backup.InterruptMaskValue = controlPWM_STATUS_MASK;
        #endif /* (controlPWM_UseStatus) */

        #if(controlPWM_DeadBandMode == controlPWM__B_PWM__DBM_256_CLOCKS || \
            controlPWM_DeadBandMode == controlPWM__B_PWM__DBM_2_4_CLOCKS)
            controlPWM_backup.PWMdeadBandValue = controlPWM_ReadDeadTime();
        #endif /*  deadband count is either 2-4 clocks or 256 clocks */

        #if(controlPWM_KillModeMinTime)
             controlPWM_backup.PWMKillCounterPeriod = controlPWM_ReadKillTime();
        #endif /* (controlPWM_KillModeMinTime) */

        #if(controlPWM_UseControl)
            controlPWM_backup.PWMControlRegister = controlPWM_ReadControlRegister();
        #endif /* (controlPWM_UseControl) */
    #endif  /* (!controlPWM_UsingFixedFunction) */
}


/*******************************************************************************
* Function Name: controlPWM_RestoreConfig
********************************************************************************
*
* Summary:
*  Restores the current user configuration of the component.
*
* Parameters:
*  None
*
* Return:
*  None
*
* Global variables:
*  controlPWM_backup:  Variables of this global structure are used to
*  restore the values of non retention registers on wakeup from sleep mode.
*
*******************************************************************************/
void controlPWM_RestoreConfig(void) 
{
        #if(!controlPWM_UsingFixedFunction)
            #if(!controlPWM_PWMModeIsCenterAligned)
                controlPWM_WritePeriod(controlPWM_backup.PWMPeriod);
            #endif /* (!controlPWM_PWMModeIsCenterAligned) */

            controlPWM_WriteCounter(controlPWM_backup.PWMUdb);

            #if (controlPWM_UseStatus)
                controlPWM_STATUS_MASK = controlPWM_backup.InterruptMaskValue;
            #endif /* (controlPWM_UseStatus) */

            #if(controlPWM_DeadBandMode == controlPWM__B_PWM__DBM_256_CLOCKS || \
                controlPWM_DeadBandMode == controlPWM__B_PWM__DBM_2_4_CLOCKS)
                controlPWM_WriteDeadTime(controlPWM_backup.PWMdeadBandValue);
            #endif /* deadband count is either 2-4 clocks or 256 clocks */

            #if(controlPWM_KillModeMinTime)
                controlPWM_WriteKillTime(controlPWM_backup.PWMKillCounterPeriod);
            #endif /* (controlPWM_KillModeMinTime) */

            #if(controlPWM_UseControl)
                controlPWM_WriteControlRegister(controlPWM_backup.PWMControlRegister);
            #endif /* (controlPWM_UseControl) */
        #endif  /* (!controlPWM_UsingFixedFunction) */
    }


/*******************************************************************************
* Function Name: controlPWM_Sleep
********************************************************************************
*
* Summary:
*  Disables block's operation and saves the user configuration. Should be called
*  just prior to entering sleep.
*
* Parameters:
*  None
*
* Return:
*  None
*
* Global variables:
*  controlPWM_backup.PWMEnableState:  Is modified depending on the enable
*  state of the block before entering sleep mode.
*
*******************************************************************************/
void controlPWM_Sleep(void) 
{
    #if(controlPWM_UseControl)
        if(controlPWM_CTRL_ENABLE == (controlPWM_CONTROL & controlPWM_CTRL_ENABLE))
        {
            /*Component is enabled */
            controlPWM_backup.PWMEnableState = 1u;
        }
        else
        {
            /* Component is disabled */
            controlPWM_backup.PWMEnableState = 0u;
        }
    #endif /* (controlPWM_UseControl) */

    /* Stop component */
    controlPWM_Stop();

    /* Save registers configuration */
    controlPWM_SaveConfig();
}


/*******************************************************************************
* Function Name: controlPWM_Wakeup
********************************************************************************
*
* Summary:
*  Restores and enables the user configuration. Should be called just after
*  awaking from sleep.
*
* Parameters:
*  None
*
* Return:
*  None
*
* Global variables:
*  controlPWM_backup.pwmEnable:  Is used to restore the enable state of
*  block on wakeup from sleep mode.
*
*******************************************************************************/
void controlPWM_Wakeup(void) 
{
     /* Restore registers values */
    controlPWM_RestoreConfig();

    if(controlPWM_backup.PWMEnableState != 0u)
    {
        /* Enable component's operation */
        controlPWM_Enable();
    } /* Do nothing if component's block was disabled before */

}


/* [] END OF FILE */
