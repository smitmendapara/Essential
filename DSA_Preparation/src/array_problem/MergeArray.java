package array_problem;

class MergeArray
{
    public int[] getConcatenation(int[] nums)
    {
        int size = nums.length;

        int temp[] = new int[size * 2];

        int n = temp.length;

        int j = 0;

        for (int i = 0; i < n; i++)
        {
            if (i < size)
            {
                temp[i] = nums[i];
            }
            else
            {
                temp[i] = nums[j];

                j++;
            }
        }
        return temp;
    }
}
