package array_problem;


public class MaxWater
{
    public static void main(String[] args)
    {
        int []arr = {1, 2, 1, 3};

        int size = arr.length;

        int min = 0;

        System.out.println(findSum(arr, size, min));

    }

    private static int findSum(int[] arr,int size,int min)
    {
        int temp = 0;

        for(int i = 0; i < size - 1; i++)
        {
            for(int j = i + 1; j < size; j++)
            {
                if(arr[i] < arr[j])
                {
                    min = arr[i] * (j - i);
                }
                else
                {
                    min = arr[j] * (j - i);
                }

                if(temp < min)
                {
                    temp = min;
                }
            }
        }
        min = temp;

        return min;
    }
}

