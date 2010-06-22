package search;


// ----------------------------------------------------------------------------
/**
 * A simple linear scan implementation of IBinarySearch (so that, in fact,
 * it is not a binary search).
 */
public class LinearSearch implements ISortedArraySearch
{
    public int find (final int [] data, final int key)
    {
        for (int i = 0; i < data.length; ++ i)
        {
            if (data [i] > key)
                return -1;
            else if (data [i] == key)
                // this line does not get covered unless there is a match:
                return i;
        }
        
        // this line does not get covered unless 'key' is larger
        // than the every element in 'data':
        return -1;
    }

} // end of class
// ----------------------------------------------------------------------------