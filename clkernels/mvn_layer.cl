#pragma OPENCL EXTENSION cl_khr_fp16 : enable
#define MIN(a,b) ((a)<(b))?(a):(b)
__kernel void mvn_kernel(
    const __global INPUT0_TYPE*  input0,
          __global OUTPUT0_TYPE* output)
{
    const uint pxpitch = MIN( INPUT0_PITCHES[2], INPUT0_PITCHES[3] );
    const uint imgsize = INPUT0_DIMS[2] * INPUT0_DIMS[3];
    const uint startidx = (get_global_id(0) / INPUT0_DIMS[1]) * INPUT0_PITCHES[0] + (get_global_id(0) % INPUT0_DIMS[1]) * INPUT0_PITCHES[1];
    const uint endidx = startidx + imgsize*pxpitch;
    const uint stride = pxpitch*LOCAL_WORKSIZE[1];
    uint lid = get_local_id(1);
    
    float sum=0;
    float sqsum=0;
    
    uint idx = startidx + lid*pxpitch;
    uint n = 0;
    while (idx<endidx) {
        sum = sum + input0[idx];
        sqsum = sqsum + input0[idx]*input0[idx];
	idx += stride;
	n += 1;
    }
    sum = sub_group_reduce_add(sum);
    sqsum = sub_group_reduce_add(sqsum);
    n = sub_group_reduce_add(n);
    
    INPUT0_TYPE mean;
    INPUT0_TYPE invvar;
    mean = sum / n;
    invvar = 1.0 / sqrt( sqsum / n - mean * mean + 0.0001);
    idx = startidx + lid*pxpitch;
    while (idx<endidx) {
        output[idx] = (input0[idx] - mean)*invvar;
	idx = idx+stride;
    }
}


// seems to work with FP32, with local group up to 1,16,1
__kernel void mvn_kernel_1(
    const __global INPUT0_TYPE*  input0,
          __global OUTPUT0_TYPE* output)
{
    const uint pxpitch = MIN( INPUT0_PITCHES[2], INPUT0_PITCHES[3] );
    const uint imgsize = INPUT0_DIMS[2] * INPUT0_DIMS[3];
    const uint startidx = (get_global_id(0) / INPUT0_DIMS[1]) * INPUT0_PITCHES[0] + (get_global_id(0) % INPUT0_DIMS[1]) * INPUT0_PITCHES[1];
    const uint endidx = startidx + imgsize*pxpitch;
    const uint stride = pxpitch*LOCAL_WORKSIZE[1];
    uint lid = get_local_id(1);
    
    INPUT0_TYPE sum=0;
    INPUT0_TYPE sqsum=0;
    
    uint idx = startidx + lid*pxpitch;
    while (idx<endidx) {
        sum = sum + input0[idx];
        sqsum = sqsum + input0[idx]*input0[idx];
	idx = idx+stride;
    }
    //sum = sub_group_reduce_add(sum);
    //sqsum = sub_group_reduce_add(sqsum);
    
    INPUT0_TYPE mean;
    INPUT0_TYPE invvar;
    mean = sum / imgsize;
    invvar = 1.0 / sqrt( sqsum / imgsize - mean * mean + 0.0001);
    idx = startidx + lid*pxpitch;
    while (idx<endidx) {
        output[idx] = (input0[idx] - mean)*invvar;
	idx = idx+stride;
    }
}
