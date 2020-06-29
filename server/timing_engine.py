from openrtist_engine import OpenrtistEngine
import time


class TimingEngine(OpenrtistEngine):
    def __init__(self, compression_params, adapter):
        super().__init__(compression_params, adapter)
        self.count = 0
        self.lasttime = time.time()
        self.lastcount = 0
        self.lastprint = self.lasttime

    def handle(self, input_frame):
        self.t0 = time.time()
        result_wrapper = super().handle(input_frame)
        self.t3 = time.time()

        self.count += 1
        if self.t3 - self.lastprint > 5:
            pre = (self.t1 - self.t0) * 1000
            infer = (self.t2 - self.t1) * 1000
            post = (self.t3 - self.t2) * 1000
            wait = (self.t0 - self.lasttime) * 1000
            fps = 1.0 / (self.t3 - self.lasttime)
            avg_fps = (self.count - self.lastcount) / (self.t3 - self.lastprint)
            print("pre {0:.1f} ms, ".format(pre), end="")
            print("infer {0:.1f} ms, ".format(infer), end="")
            print("post {0:.1f} ms, ".format(post), end="")
            print("wait {0:.1f} ms, ".format(wait), end="")
            print("fps {0:.2f}".format(fps))
            print("avg fps: {0:.2f}".format(avg_fps))
            print()
            self.lastcount = self.count
            self.lastprint = self.t3

        self.lasttime = self.t3

        return result_wrapper

    def inference(self, preprocessed):
        self.t1 = time.time()
        post_inference = super().inference(preprocessed)
        self.t2 = time.time()

        return post_inference
