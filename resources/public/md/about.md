# 原理

抽奖有一个截止时间,在截止时间后将所有参与者的ID加上所有参与者自己添加的随机字符串整合为抽奖源数据.

接下来用这个源数据做sha256的算法,然后mod上用户数,得到随机抽取出的用户编号.

对于抽奖结果有疑问的话可以直接自行跑算法来验证,并且从数据源里可以自行挖掘作弊者.
