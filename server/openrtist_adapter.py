from abc import abstractmethod
from abc import ABC


class OpenrtistAdapter(ABC):
    def __init__(self, default_style):
        self._style = default_style

    @abstractmethod
    def preprocessing(self, img):
        '''Model-specific preprocessing'''
        pass

    @abstractmethod
    def inference(self, preprocessed):
        pass

    @abstractmethod
    def postprocessing(self, post_inference):
        '''Model-specific postprocessing'''
        pass

    def set_style(self, new_style):
        self._style = new_style

    def get_style(self):
        return self._style

    def _style_image(self):
        return ""

    def get_style_image(self):
        try:
            with open(self._style_image, "rb") as f:
                return f.read()
        except:
            return b''

    def get_all_styles(self):
        return []
