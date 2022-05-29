package array_problem;

class RemoveDuplicate
{
    public int removeDuplicates(int[] nums)
    {
        int size = nums.length;

        if (size == 0)
        {
            return 0;
        }

        int i;

        int index = 1;

        int first = nums[0];

        for (i = 1; i < size; i++)
        {
            if (first != nums[i])
            {
                first = nums[i];

                nums[index] = nums[i];

                index++;
            }
        }
        return index;
    }
}
