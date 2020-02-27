/*******************************************************************************
* File Name: dig_out.h  
* Version 2.20
*
* Description:
*  This file contains the Alias definitions for Per-Pin APIs in cypins.h. 
*  Information on using these APIs can be found in the System Reference Guide.
*
* Note:
*
********************************************************************************
* Copyright 2008-2015, Cypress Semiconductor Corporation.  All rights reserved.
* You may use this file only in accordance with the license, terms, conditions, 
* disclaimers, and limitations in the end user license agreement accompanying 
* the software package with which this file was provided.
*******************************************************************************/

#if !defined(CY_PINS_dig_out_ALIASES_H) /* Pins dig_out_ALIASES_H */
#define CY_PINS_dig_out_ALIASES_H

#include "cytypes.h"
#include "cyfitter.h"
#include "cypins.h"


/***************************************
*              Constants        
***************************************/
#define dig_out_0			(dig_out__0__PC)
#define dig_out_0_PS		(dig_out__0__PS)
#define dig_out_0_PC		(dig_out__0__PC)
#define dig_out_0_DR		(dig_out__0__DR)
#define dig_out_0_SHIFT	(dig_out__0__SHIFT)
#define dig_out_0_INTR	((uint16)((uint16)0x0003u << (dig_out__0__SHIFT*2u)))

#define dig_out_INTR_ALL	 ((uint16)(dig_out_0_INTR))


#endif /* End Pins dig_out_ALIASES_H */


/* [] END OF FILE */
