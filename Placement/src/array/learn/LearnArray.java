package array.learn;

public class LearnArray
{
    private int[] intArray;

    private int count;

    LearnArray(int length)
    {
        intArray = new int[length];
    }

    public void print()
    {
        for (int i = 0; i < count; i++)
        {
            System.out.println(intArray[i]);
        }

        System.out.println(intArray.length);
    }

    public void insert(int item)
    {
        if (intArray.length == count)  // check intArray size
        {
            int newArray[] = new int[count * 2];    // create new array with double size

            for (int i = 0; i < count; i++)
            {
                newArray[i] = intArray[i];
            }

            intArray = newArray;
        }

        intArray[count++] = item; // add item to the array
    }

    public void removeAt(int index)
    {
        if(index < 0 || index >= count) // check index is exists or not
        {
            throw new IllegalArgumentException();
        }

        for(int i = index; i < count; i++)
        {
            intArray[i] = intArray[i + 1];    // shift element to the deleted element
        }

        count--; // to reduce one size of an array
    }

    public int indexOf(int value)
    {
        for(int i = 0; i < count; i++)
        {
            if(intArray[i] == value)
            {
                return i;
            }
        }

        return -1;
    }

    public static void main(String[] args)
    {
        try
        {
            LearnArray arr = new LearnArray(4);

            arr.insert(10);

            arr.insert(20);

            arr.insert(30);

            arr.removeAt(1);

            System.out.println(arr.indexOf(30));

            arr.print();
        }

        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

}



