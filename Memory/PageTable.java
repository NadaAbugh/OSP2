package osp.Memory;
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

/**
The PageTable class represents the page table for a given task.
A PageTable consists of an array of PageTableEntry objects.  This
page table is of the non-inverted type.

@OSPProject Memory
 */

public class PageTable extends IflPageTable
{
	/**
	   The page table constructor. Must call

	       super(ownerTask)

	   as its first statement. Then it must figure out
	   what should be the size of a page table, and then
	   create the page table, populating it with items of
	   type, PageTableEntry.

	   @OSPProject Memory
	 */
	// Author: Dareen Bukhari   - 1607281 
	//         Nada Abughazalah - 1606827
	// Last Update: April 8, 2020 
	public PageTable(TaskCB ownerTask) {
		super(ownerTask); // Calls the superclass constructor

		int pageTableSize = (int) Math.pow(2, MMU.getPageAddressBits()); // Page table size = 2^(number of bits)

		pages = new PageTableEntry[pageTableSize]; // Create page table (pages is of type PageTable)
		for (int i=0; i<pageTableSize; i++) // Populate pages with PageTableEntry items
			pages[i] = new PageTableEntry(this, i); // Each entry is initialized with this PT and its number is i
	}

	/**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
	 */  
	// Author: Nada Abughazalah - 1606827
	// Last Update: April 8, 2020
	public void do_deallocateMemory() {
		for (int i=0; i<MMU.getFrameTableSize(); i++) {
			if(MMU.getFrame(i).getPage() != null ) {
				if (getTask() == MMU.getFrame(i).getPage().getTask()) { //NOT SURE ABOUT THIS
					MMU.getFrame(i).setPage(null); // Make the 'page' that points to the page occupying the frame null
					MMU.getFrame(i).setDirty(false); // Clear the  dirty bit 
					MMU.getFrame(i).setReferenced(false); // Clear the reference bit	
				} 
				if (MMU.getFrame(i).getReserved() == getTask()) // If frame is reserved by task
					MMU.getFrame(i).setUnreserved(getTask()); // Un-reserve frame
			}
		}
	}
}

