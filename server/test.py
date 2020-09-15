import cv2
import numpy as np
# import logging
# from gabriel_server import cognitive_engine
# from gabriel_protocol import gabriel_pb2
# from openrtist_protocol import openrtist_pb2


# load the image
# np_data = np.fromstring(image, dtype=np.uint8)
# img = cv2.imdecode(np_data, cv2.IMREAD_COLOR)
# img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
img = cv2.imread('image.jpg')
# img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

# # detect circles
# gray = cv2.medianBlur(cv2.cvtColor(img, cv2.COLOR_RGB2GRAY), 5)
# circles = cv2.HoughCircles(gray, cv2.HOUGH_GRADIENT, 1, 20, param1=50, param2=50, minRadius=0, maxRadius=0)
# circles = np.uint16(np.around(circles))

# depth_map = cv2.imread('depth.jpg', cv2.IMREAD_GRAYSCALE)
# depth_map = cv2.cvtColor(depth_map, cv2.COLOR_BGR2RGB)

# perform depth thresholding to create foreground mask with 3 channels
# lower_blue = (65,75,40)
# upper_blue = (95,110,65)
lower_blue = (40,81,30)
upper_blue = (120,150,95)
mask = cv2.inRange(img,lower_blue,upper_blue)
# mask = cv2.merge([mask,mask,mask])

# # Apply morphology to the thresholded image to remove extraneous white regions and save a mask
kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5,5))
mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)
mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)

# cv2.imshow("mask before", mask)

# get first masked value (foreground)
fg = cv2.bitwise_and(img, img, mask= mask)

# get background mask by inversion
mask_inv = cv2.bitwise_not(mask)
bg = cv2.bitwise_and(img, img, mask= mask_inv)

# transform the background

height, width, depth = fg.shape
print(height, width, depth)
print(type(fg))

height1, width1, depth1 = bg.shape
print(height1, width1, depth1)
print(type(bg))

# combine foreground+background
final = np.bitwise_or(fg, bg)

# display it
cv2.imshow("mask", mask)
cv2.imshow("mask_inv", mask_inv)
cv2.imshow("foreground", fg)
cv2.imshow("background", bg)
cv2.imshow("result", final)
cv2.waitKey(0)
cv2.destroyAllWindows()
