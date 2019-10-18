from openrtist_engine import OpenrtistEngine
from torch.autograd import Variable
from transformer_net import TransformerNet
from torchvision import transforms
import torch


class TorchEngine(OpenrtistEngine):
    def __init__(self, use_gpu, default_style, compression_params):
        super().__init__(default_style, compression_params)

        self.use_gpu = use_gpu

        self.style_model = TransformerNet()
        self.style_model.load_state_dict(torch.load(self.model))

        if (self.use_gpu):
            self.style_model.cuda()

        self.content_transform = transforms.Compose([transforms.ToTensor()])

    def change_style(self, new_style):
        self.model = self.path + new_style + ".model"
        self.style_model.load_state_dict(torch.load(self.model))
        if (self.use_gpu):
            self.style_model.cuda()

    def preprocessing(self, img):
        content_image = self.content_transform(img)
        if (self.use_gpu):
            content_image = content_image.cuda()
        content_image = content_image.unsqueeze(0)
        return Variable(content_image, volatile=True)


    def inference(self, preprocessed):
        output = self.style_model(preprocessed)
        return output.data[0].clamp(0, 255).cpu().numpy()

    def postprocessing(self, post_inference):
        return post_inference.transpose(1, 2, 0)
