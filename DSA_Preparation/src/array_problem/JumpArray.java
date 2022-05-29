package array_problem;

class JumpArray
{
    public static void main(String[] args)
    {
        int[] intArray = {2, 3, 1, 1, 4};

        boolean result = canJump(intArray);

        System.out.println(result);
    }

    public static boolean canJump(int[] intArray)
    {
        int val = 0;

        for (int i = 0; i < intArray.length; i++)
        {
            if (val < i)
            {
                return false;
            }
            val = Math.max(val, intArray[i] + i);
        }

        return true;
    }
}

/*
    another solution

    public boolean canJump(int[] nums) {

        int length = nums.length - 1;

        int i = 0;

        while (i <= length)
        {
            if (i == length)
            {
                return true;
            }
            else if (nums[i] == 0)
            {
                return false;
            }
            else
            {
                i += nums[i];
            }
        }
        return false;
    }
*/

