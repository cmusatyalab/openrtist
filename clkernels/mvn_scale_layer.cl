#pragma OPENCL EXTENSION cl_khr_fp16 : enable
#define MIN(a,b) ((a)<(b))?(a):(b)
#define MAX(a,b) ((a)>(b))?(a):(b)
__kernel void mvn_scale_kernel(
    const __global INPUT0_TYPE*  input0,
          __global OUTPUT0_TYPE* output,
    const __global INPUT0_TYPE* weights,
    const __global INPUT0_TYPE* biases)
{
    const uint channel = get_global_id(0) % INPUT0_DIMS[1];
    const uint startidx = (get_global_id(0) / INPUT0_DIMS[1]) * INPUT0_PITCHES[0] + channel * INPUT0_PITCHES[1] + INPUT0_OFFSET;
    const uint startidx2 = (get_global_id(0) / OUTPUT0_DIMS[1]) * OUTPUT0_PITCHES[0] + channel * OUTPUT0_PITCHES[1] + OUTPUT0_OFFSET;
    const uint lid = get_local_id(1);
    
    float sum=0;
    float sqsum=0;
    
    uint n = 0;
    uint y = 0;
    uint x = lid;
    while (y<INPUT0_DIMS[2]) {
      while (x<INPUT0_DIMS[3]) {
        uint idx = startidx + y*INPUT0_PITCHES[2] + x*INPUT0_PITCHES[3];
        sum = sum + input0[idx];
        sqsum = sqsum + input0[idx]*input0[idx];
	x += LOCAL_WORKSIZE[1];
	n += 1;
      }
      y += x / INPUT0_DIMS[3];
      x = x % INPUT0_DIMS[3];
    }
    sum = sub_group_reduce_add(sum);
    sqsum = sub_group_reduce_add(sqsum);
    n = sub_group_reduce_add(n);
    
    INPUT0_TYPE mean = sum / n;
    INPUT0_TYPE scale = weights[channel] / sqrt( sqsum / n - mean * mean + 0.0001);
    INPUT0_TYPE shift = biases[channel] - mean*scale;
    OUTPUT0_TYPE value;

    y = 0;
    x = lid;
    while (y<OUTPUT0_DIMS[2]) {
      while (x<OUTPUT0_DIMS[3]) {
        uint idx = startidx + y*INPUT0_PITCHES[2] + x*INPUT0_PITCHES[3];
        uint idx2 = startidx2 + y*OUTPUT0_PITCHES[2] + x*OUTPUT0_PITCHES[3];
        value = input0[idx]*scale + shift;
#if USE_RELU!=0
        output[idx2] = MAX(value, 0.0);
#else
        output[idx2] = value;
#endif
	x += LOCAL_WORKSIZE[1];
      }
      y += x / OUTPUT0_DIMS[3];
      x = x % OUTPUT0_DIMS[3];
    }
}


