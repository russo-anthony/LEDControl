/* ========================================
 *
 * Copyright YOUR COMPANY, THE YEAR
 * All Rights Reserved
 * UNPUBLISHED, LICENSED SOFTWARE.
 *
 * CONFIDENTIAL AND PROPRIETARY INFORMATION
 * WHICH IS THE PROPERTY OF your company.
 *
 * ========================================
*/
#include "project.h"

uint8_t count = 255;

int main(void)
{
    CyGlobalIntEnable; /* Enable global interrupts. */

    /* Place your initialization/startup code here (e.g. MyInst_Start()) */

    Clock_1_Start();
    PWM_1_Start();
    isr_1_Start();
    
    
    for(;;)
    {
        /* Place your application code here. */
        count--;
        PWM_1_WriteCompare(count);
        CyDelay(10);
    }
}

/* [] END OF FILE */
