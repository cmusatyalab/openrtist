
            if not os.path.exists(self._rgbpipe_path):
                os.mkfifo(self._rgbpipe_path)
            print("Opening fifo {} for writing... Block waiting for reader...".format(self._rgbpipe_path))
            self._rgbpipe = open(self._rgbpipe_path, 'w')
            print("Fifo opened.")

while True:
    # get frame
                        style_image = style_name_to_image[resp_header['style']]
                    style_im_h, style_im_w, _ = style_image.shape
                    rgb_frame[0:style_im_h, 0:style_im_w, :] = style_image
                    cv2.rectangle(rgb_frame, (0,0), (int(style_im_w), int(style_im_h)), (255,0,0), 3)
                    rgb_frame_enlarged = cv2.resize(rgb_frame, (960, 540))
                    self._rgbpipe.write(rgb_frame_enlarged.tostring())


def main():
    """Setup for GHC 9th project room screen saver.

    When launched directly, this script sends style-transferred video stream to xscreensaver through a named FIFO.
    """
    parser = argparse.ArgumentParser()
    parser.add_argument("server_ip", action="store", help="IP address for Openrtist Server")
    parser.add_argument("output_pipe_path", action="store", help="The linux pipe the style-transferred images will be streamed to")
    inputs = parser.parse_args()
    style_name_to_image = _load_style_images('style-image')
    gabriel_client = GabrielClient(inputs.server_ip, recv_pipe_path=inputs.output_pipe_path)
    gabriel_client.start()


if __name__ == '__main__':
    main()
