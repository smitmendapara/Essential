package array_problem;

import java.util.Arrays;

public class KthMaxAndMin
{
    public static void main(String[] args)
    {
        int arr[] = {2, 14, 8, 16, 6};

        int size = arr.length;

        int k = 4;

        Arrays.sort(arr);

        System.out.println(k + "nd Smallest Element is : " +smallestElement(arr, k));

        System.out.println(k + "nd Largest Element is : " +largestElement(arr, k, size));

    }

    private static int smallestElement(int[] arr, int k)
    {
        return arr[k - 1];
    }

    private static int largestElement(int[] arr, int k, int size)
    {
        return  arr[size - k];
    }
}

