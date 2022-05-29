package array_problem;

public class ReverseArray
{
    public static void main(String[] args)
    {
        int[] intArray = {5, 3, 2, 4, 1};

        int start = 0;

        int end = intArray.length - 1;

        reverseArray(intArray, start, end);

        printArray(intArray);
    }

    private static void printArray(int intArray[])
    {
        for (int i = 0; i < intArray.length; i++)
        {
            System.out.print(intArray[i] + ", ");
        }
    }

    private static void reverseArray(int intArray[], int start, int end)
    {
        if (start < end)
        {
            int temp = intArray[start];

            intArray[start] = intArray[end];

            intArray[end] = temp;

            reverseArray(intArray, start + 1, end - 1);
        }
    }
}
