package osp.Memory; 
import osp.Tasks.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.IflFrameTableEntry;

/**
The FrameTableEntry class contains information about a specific page
frame of memory.

@OSPProject Memory
*/

public class FrameTableEntry extends IflFrameTableEntry {

    /**
       The frame constructor. Must have

       	   super(frameID)

       as its first statement.

       @OSPProject Memory
    */
	
	// Author: Dareen Bukhari - 1607281
	// Last Update: April 6, 2020
	
    public FrameTableEntry(int frameID) {

    	 super(frameID); // Calls the superclass constructor 
    }
}
