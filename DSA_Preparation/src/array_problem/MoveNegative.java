package array_problem;

public class MoveNegative
{
    public static void main(String[] args) {

        int[] array = {1, 3, -2, -4, -2, 5, -2};

        moveElement(array);

        for (int i = 0; i < array.length; i++)
        {
            System.out.println(array[i]);
        }
    }

    private static void moveElement(int[] array)
    {
        int j = 0;

        for (int i = 0; i < array.length; i++)
        {
            if (array[i] < 0)
            {
                if (i != j)
                {
                    int temp = array[i];

                    array[i] = array[j];

                    array[j] = temp;
                }
                j++;
            }
        }
    }
}
