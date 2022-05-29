package array_problem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

class ThreeSum
{
    public List<List<Integer>> threeSum(int[] nums)
    {
        HashSet<List<Integer>> set = new HashSet<>();

        int i = 0;

        Arrays.sort(nums);

        while(i < nums.length - 2)
        {
            int j = i + 1;

            int k = nums.length - 1;

            while(i < j)
            {
                if(nums[i] + nums[j] + nums[k] == 0)
                {
                    set.add(new ArrayList(Arrays.asList(nums[i], nums[j], nums[k])));

                    j++;

                    k--;
                }
                else if(nums[i] + nums[j] + nums[k] < 0)
                {
                    j++;
                }
                else
                {
                    k--;
                }
                i++;
            }
        }
        return new ArrayList(set);
    }
}
