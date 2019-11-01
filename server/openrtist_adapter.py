from abc import abstractmethod
from abc import ABC
import logging
import os

logger = logging.getLogger(__name__)


class OpenrtistAdapter(ABC):
    def __init__(self, default_style):
        self._style = None
        self.path = "."
        self.supported_styles = {}

    @abstractmethod
    def preprocessing(self, img):
        """Model-specific preprocessing"""
        pass

    @abstractmethod
    def inference(self, preprocessed):
        pass

    @abstractmethod
    def postprocessing(self, post_inference):
        """Model-specific postprocessing"""
        pass

    def add_supported_style(self, new_style):
        try:
            with open(os.path.join(self.path, "{}.txt".format(new_style)), "r") as f:
                info_text = f.read()
        except IOError:
            info_text = new_style + " -- Unknown"
        self.supported_styles[new_style] = info_text
        if self._style is None:
            self.set_style(new_style)

    def set_style(self, new_style):
        if new_style not in self.supported_styles:
            logger.error("Got style %s that we do not have. Ignoring", new_style)
            return False
        self._style = new_style
        return True

    def get_style(self):
        return self._style

    def _style_image(self):
        return os.path.join(self.path, "{}.jpg".format(self._style))

    def get_style_image(self):
        try:
            with open(self._style_image(), "rb") as f:
                return f.read()
        except IOError:
            return b""

    def get_all_styles(self):
        return self.supported_styles
