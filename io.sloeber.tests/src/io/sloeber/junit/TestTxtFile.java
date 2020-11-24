package io.sloeber.junit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.sloeber.core.txt.KeyValueTree;

@SuppressWarnings({ "nls", "static-method" })
public class TestTxtFile {


    @Test
    public void dumpMatchesExpectationFull() {
        KeyValueTree root = getDataSet1();
        String result = root.dump();
        String expectedResult = "key2=value2\n" + "key2.key2_1=value2_1\n" + "key1.key1_1=value1_1\n"
                + "key1.key1_2=value1_2\n";
        assertEquals("creation", expectedResult, result);
    }
    @Test
    public void dumpMatchesExpectationFull2() {
        KeyValueTree root = getDataSet1();
        root.addValue("he.I.Can.Do.This", "not once");
        root.addValue("he.I.Can.Hit.This", "not twice");
        root.addValue("he.I.Can.hit.This", "not three times");
        root.addValue("Something.Completely.different", "but always");
        String result = root.dump();
        String expectedResult = "key2=value2\n" + "key2.key2_1=value2_1\n" + "key1.key1_1=value1_1\n"
                + "key1.key1_2=value1_2\n" + "he.I.Can.Do.This=not once\n" + "he.I.Can.Hit.This=not twice\n"
                + "he.I.Can.hit.This=not three times\n" + "Something.Completely.different=but always\n";
        assertEquals("creation", expectedResult, result);
    }

    @Test
    public void dumpMatchesExpectationKey1() {
        KeyValueTree root = getDataSet1();
        KeyValueTree key1 = root.getChild("key1");
        String result = key1.dump();
        String expectedResult = "key1_1=value1_1\n" + "key1_2=value1_2\n";
        assertEquals("select key1", expectedResult, result);
    }


    @Test
    public void dumpMatchesExpectationKey2() {
        KeyValueTree root = getDataSet1();
        KeyValueTree key1 = root.getChild("key2");
        String result = key1.dump();
        String expectedResult = "key2_1=value2_1\n";
        assertEquals("select key 2", expectedResult, result);
    }
    @Test
    public void dumpMatchesExpectationKey3() {
        KeyValueTree root = getDataSet2();
        KeyValueTree keyHe = root.getChild("he");
        KeyValueTree keyI = keyHe.getChild("I");
        KeyValueTree keyCan = keyI.getChild("Can");
        String result = keyCan.dump();
        String expectedResult = "Do.This=not once\n" + "Hit.This=not twice\n" + "hit.This=not three times\n";
        assertEquals("select key 2", expectedResult, result);
    }

    @Test
    public void dumpMatchesExpectationWrongKey() {
        KeyValueTree root = getDataSet1();
        KeyValueTree key1 = root.getChild("NotExistingKey");
        String result = key1.dump();
        String expectedResult = "";
        assertEquals("Wrong Key", expectedResult, result);
    }

    @Test
    public void getValuekey1() {
        KeyValueTree root = getDataSet1();
        String result = root.getValue("key2");
        String expectedResult = "value2";
        assertEquals("Wrong getvalue", expectedResult, result);
    }

    @Test
    public void getValuekey2() {
        KeyValueTree root = getDataSet1();
        String result = root.getValue("key1.key1_2");
        String expectedResult = "value1_2";
        assertEquals("Wrong getvalue", expectedResult, result);
    }

    @Test
    public void getValuekey3() {
        KeyValueTree root = getDataSet1();
        String result = root.getValue("key1.key1_2");
        String expectedResult = "value1_2";
        assertEquals("Wrong getvalue", expectedResult, result);
    }

    private KeyValueTree getDataSet1() {
        KeyValueTree root = KeyValueTree.createTxtRoot();

        KeyValueTree key2 = root.addChild("key2", "value2");
        key2.addChild("key2_1", "value2_1");
        KeyValueTree key1 = root.addChild("key1");
        key1.addChild("key1_1", "value1_1");
        key1.addChild("key1_2", "value1_2");
        return root;
    }

    private KeyValueTree getDataSet2() {
        KeyValueTree root = getDataSet1();
        root.addValue("he.I.Can.Do.This", "not once");
        root.addValue("he.I.Can.Hit.This", "not twice");
        root.addValue("he.I.Can.hit.This", "not three times");
        root.addValue("Something.Completely.different", "but always");
        return root;
    }
}
