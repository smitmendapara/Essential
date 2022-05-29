package array_problem;

public class SortZeroOneTwo
{
    public static void main(String[] args)
    {
        int[] intArray = {2,0,1};

        int start = 0;

        int mid = 0;

        int end = intArray.length - 1;

        sortNumber(intArray, start, mid, end);

        printArray(intArray);
    }

    private static void printArray(int[] intArray)
    {
        for (int i = 0; i < intArray.length; i++)
        {
            System.out.println(intArray[i]);
        }
    }

    private static void sortNumber(int[] intArray, int start, int mid, int end)
    {
        int temp = 0;

        while (mid <= end)
        {
            if (intArray[mid] == 0)
            {
                temp = intArray[start];

                intArray[start] = intArray[mid];

                intArray[mid] = temp;

                start++;

                mid++;
            }
            else if (intArray[mid] == 2)
            {
                temp = intArray[mid];

                intArray[mid] = intArray[end];

                intArray[end] = temp;

                end--;
            }
            else
            {
                mid++;
            }
        }
    }
}
