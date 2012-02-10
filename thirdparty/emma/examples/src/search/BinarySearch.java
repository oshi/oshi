package search;


// ----------------------------------------------------------------------------
/**
 * A binary search implementation.
 */
public class BinarySearch implements ISortedArraySearch
{
    // public: ................................................................
    
    public BinarySearch () {}
    
    public int find (final int[] data, final int key)
    {
        int low = 0, high = data.length - 1;
        
        while (low <= high)
        {
            final int i = (low + high) >> 1;
            final int v = data [i];
            
            if (v == key)
                return i; // this line does not get covered unless there is a match
            else if (v < key)
                low = i + 1;
            else // v > key
                high = i - 1;
        }
        
        return -1;
    }

} // end of class
// ----------------------------------------------------------------------------