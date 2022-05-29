package array_problem;

class TwiceDuplicate
{
    public static void main(String[] args)
    {
        System.out.println(singleNonDuplicate());;
    }

    private static int singleNonDuplicate()
    {
        int []nums = {1, 1, 2, 3, 3, 4, 4};

        int size = nums.length;

        int unique = 0;

        int i;

        if (size == 1)
        {
            return nums[size - 1];
        }

        for (i = 0; i < size - 2; i+=2)
        {
            if (nums[i] == nums[i + 1])
            {
                continue;
            }
            else
            {
                unique = nums[i];

                return unique;
            }
        }

        if (unique == 0 && nums[i] != nums[i - 1])
        {
            unique = nums[i];
        }

        return unique;
    }
}


