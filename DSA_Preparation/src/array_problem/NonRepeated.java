package array_problem;

import java.util.NoSuchElementException;

public class NonRepeated
{
    public static void main(String[] args)
    {
        int arr[] = {1, 1, 1, 1, 2, 2, 3, 3, 3, 4, 4, 5};

        int size = arr.length;

        System.out.println(findUnique(arr, size));
    }

    private static int findUnique(int arr[],int size)
    {
        int unique = 0;

        int i;

        if(size == 1)
        {
            return arr[unique];
        }

        for(i = 1; i < size - 1; i++)
        {
            if(arr[i] == arr[i - 1] || arr[i] == arr[i + 1])
            {
                continue;
            }
            else
            {
                unique = arr[i];
            }
        }

        if(unique == 0 && arr[i] != arr[i - 1])
        {
            unique = arr[i];

            return unique;
        }

        throw new NoSuchElementException("Element not found!");
    }
}

