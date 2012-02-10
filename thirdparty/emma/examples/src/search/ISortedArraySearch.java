package search;

// ----------------------------------------------------------------------------
/**
 * A simple interface for searching arrays of integers that are sorted in
 * increasing order.
 */
public interface ISortedArraySearch
{
    // public: ................................................................
    
    /**
     * Search 'data' for a value matching 'key' and return
     * its index if found. Return  -1 if no match is found. 
     */
    int find (int [] data, int key);

} // end of interface
// ----------------------------------------------------------------------------