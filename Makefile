SRC = src/
BIN = bin/
JC = javac
JC_FLAGS = -g
RM_FLAGS = rm -rf

default: $(SRC)*.java
	$(JC) $(JC_FLAGS) $(SRC)*.java
	mkdir $(BIN)
	mv $(SRC)*.class $(BIN)

clean:
	$(RM_FLAGS) $(BIN)
