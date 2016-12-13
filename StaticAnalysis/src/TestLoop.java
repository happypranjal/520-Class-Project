public class TestLoop {
    public void foo() {
    	int k = 10;
    	if(k<4){
    		System.out.println(k);
    	}
        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < 5; i++) {
                System.out.println(i);
            }
        }
        while(k>1){
        	System.out.println("Whatttt");
        	k--;
        }
    }
}