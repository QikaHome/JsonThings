$tellraw @s ["Player $(Name) clicked at X:$(X) Y:$(Y) Z:$(Z) with hand $(Hand)"]
$execute unless items entity @s $(Hand) *[count] run return 1
give @s apple 1
return 0