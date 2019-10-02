module Tree where

data Tree a = Nil | Node (Tree a) a (Tree a)
  deriving (Eq, Show)

--data Tree a where
--  Nil :: Tree a
--  Node :: Tree a -> a -> Tree a -> Tree a

bottom :: a -> Tree a
bottom x = Node Nil x Nil

make_list_left :: [a] -> Tree a
make_list_left [] = Nil
make_list_left (x:xs) = Node (make_list_left xs) x Nil

make_list_right :: [a] -> Tree a
make_list_right [] = Nil
make_list_right (x:xs) = Node Nil x (make_list_right xs)

read_left :: Tree a -> [a]
read_left Nil = []
read_left (Node l x r) = x:(read_left l)

read_right :: Tree a -> [a]
read_right Nil = []
read_right (Node l x r) = x:(read_right r)
