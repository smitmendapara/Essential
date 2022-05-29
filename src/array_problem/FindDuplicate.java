package array_problem;

import java.util.LinkedList;

public class FindDuplicate
{
    public static void main(String[] args)
    {
        int []array = {1, 2, 3, 2, 5, 4, 3};

        int size = array.length - 1;

        int k = findDuplicate(array, size);

        print(array, k);
    }

    private static void print(int[] array, int k)
    {
        for (int i = 0; i < k; i++)
        {
            System.out.println(array[i]);
        }
    }

    private static int findDuplicate(int[] array, int size)
    {
        int k = 0;

        LinkedList<Integer> list = new LinkedList<>();

        for (int i = 0; i <= size; i++)
        {
            if (!list.contains(array[i]))
            {
                list.add(array[i]);
            }
            else
            {
                array[k] = array[i];

                k++;
            }
        }

        return k;
    }
}
