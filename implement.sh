#!/bin/sh

FILE=`basename $1`
echo "Implementing $FILE on yoda, luke and han..."
scp -i han.ppk /tmp/allshoppings/lib/$FILE getin@yoda.getin.mx:/usr/local/allshoppings/lib/
scp -i han.ppk /tmp/allshoppings/lib/$FILE getin@luke.getin.mx:/usr/local/allshoppings/lib/
scp -i han.ppk /tmp/allshoppings/lib/$FILE getin@han.getin.mx:/usr/local/allshoppings/lib/
